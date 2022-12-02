import csv
import os
import queue
import threading
import cv2
import requests
import shutil
import olympe
from olympe.video.renderer import PdrawRenderer
from olympe.messages.camera import (
	set_camera_mode,
	set_recording_mode,
	set_streaming_mode,
	set_photo_mode,
	take_photo,
	stop_photo,
	photo_progress,
	start_recording,
	stop_recording,
	recording_progress,
)

# << Camera Photo, Recording and Stream Methods >>			
class AnafiCameraMedia:
	'''
	Wrapper for the Parrot Olympe camera methods involving media
	
	...
	
	Attributes
	----------
	drone : olympe.Drone
		the drone object
	drone_ip : str
		the drone's ip_address
	drone_rtstp_port : str
		the drone's rtsp port which connects to the live stream
	drone_url : str
		the url used request to make requests from the drone
	drone_media_api_url : str
		the complete url used to make media requests from the drone
	download_dir : str
		The location drone media will be downloaded
	camera_mode : str
		the drone's current camera mode (None, photo, recording, streaming)
		
	Methods
	-------
	setup_photo(mode, format, file_format, burst, bracketing, capture_interval)
		Prepares the drone camera for photos, and changes camera_mode to "photo"
	take_photo()
		Takes a photo
	start_lapse_photo()
		Starts to take time/gps lapse photos
	stop_lapse_photo()
		Stops current time/gps lapse photos
	setup_recording(mode, resolution, framerate, hyperlapse)
		Prepares the drone camera for recording, and changes camera_mode to "recording"
	start_recording()
		Starts a recording
	stop_recording()
		Stops current recording
	download_last_media()
		Downloads the last media taken
	setup_stream(value, record, yuv_frame_processing, yuv_frame_cb, h264_frame_cb, start_cb, end_cb, flush_cb)
		Prepares the drone camera for streaming, and changes camera_mode to "streaming"
	start_stream()
		Starts the live video stream
	strop_stream()
		Stops the current live video stream
	'''

	def __init__(self, drone_object, drone_ip, drone_rtsp_port, drone_url, download_dir):
		'''
		Parameters
		----------
		drone_object : olympe.Drone
			the drone object
		drone_ip : str
			the drone's ip_address
		drone_rtstp_port : str
			the drone's rtsp port which connects to the live stream
		drone_url : str
			the url used request to make requests from the drone
		download_dir : str
			The location drone media will be downloaded
		'''
		
		self.drone = drone_object
		self.drone_ip = drone_ip		
		self.drone_rtsp_port = drone_rtsp_port
		self.drone_url = drone_url
		self.drone_media_api_url = self.drone_url + "api/v1/media/medias/"

		self.download_dir = download_dir
		self.camera_mode = "None"
	
	# << Photo Methods >>
	def setup_photo(self,
		mode = "single",
		format= "rectilinear",
		file_format="dng",
		burst="burst_14_over_1s",
		bracketing="preset_1ev",
		capture_interval=0.1
	):
		'''
		Prepares the drone camera for photos, and changes camera_mode to "photo"
		
		Parameters
		----------
		mode : str, optional
			the photo mode (default = "single")
			- "single"/"bracketing"/"burst"/"time_lapse"/"gps_lapse"
		format : str, optional
			the photo format (default = "rectilinear")
			- "full_frame"/"rectilinear"
		file_format : str, optional
			the photo file format (default = "dng")
			- "jpeg"/"dng"/"dng_jpeg"
		burst : str, optional
			The amount of photos/second to take (default = "burst_14_over_1s")
			only used if mode is "burst"
			- "burst_X_over_Xs" (14/10/4 , 4/2/1)
		bracketing : str, optional
			The exposure values of the bracketed photos (default = "preset_1ev")
			only used if mode is "bracketing"
			- preset_Xev (1/2/3)
			- preset_Xev_Xev (1 , 2/3)/(2 , 3)
			- preset_Xev_Xev_Xev (1 , 2 , 3)
		capture_interval : float, optional
			The interval between each photo (default = 0.1)
			seconds in time lapse and meters in gps lapse
			only used if mode is "time_lapse" or "gps_lapse"
		'''
	
		self.drone(set_camera_mode(cam_id=0, value="photo")).wait()
		self.drone(set_photo_mode(
			cam_id=0,
			mode= mode,
			format= format,
			file_format= file_format,
			burst= burst,
			bracketing= bracketing,
			capture_interval= capture_interval,
		)).wait()
		self.camera_mode = "photo"
		print("< Photo Mode >")
	
	def take_photo(self):
		'''
		Takes a photo
		'''
		if (self.camera_mode != "photo"):
			setup_photo()
		self.media_saved = self.drone(photo_progress(result="photo_saved", _policy="wait"))
		self.drone(take_photo(cam_id=0)).wait()
		self.media_saved.wait()
		path = self.download_last_media()
		print("< Take Photo >")
		return path
	
	def start_lapse_photo(self):
		'''	
		Starts to take time/gps lapse photos. Continues to take pictures until "stop_lapse_photo" is called
		Should only be used when camera mode is set to eiher "time_lapse" or "gps_lapse"
		'''
		if (self.camera_mode != "photo"):
			setup_photo()
		self.media_saved = self.drone(photo_progress(result="photo_saved", _policy="wait"))
		self.drone(take_photo(cam_id=0)).wait()
		print("< Lapse Photo Started >")

	def stop_lapse_photo(self):
		'''
		Stops current time/gps lapse photos
		Should only be used when camera mode is set to eiher "time_lapse" or "gps_lapse"
		'''
		
		self.drone(stop_photo(cam_id=0)).wait()
		self.media_saved.wait()
		path = self.download_last_media()
		print("< Lapse Photo Stopped >")
		return path
		

	# << Recording Methods >>
	def setup_recording(self,
		mode = "standard",
		resolution = "res_dci_4k",
		framerate = "fps_24",
		hyperlapse = "ratio_15",
	):
		'''
		Prepares the drone camera for recording, and changes camera_mode to "recording"
		
		Parameters
		----------
		mode : str, optional
			the recording mode (default = "standard")
			- "standard"/"hyperlapse"/"slow_motion"/"high_framerate"
		resolution : str, optional
			the recording resolution (default = "res_dci_4k")
			- "res_dci_4k" (4K Cinema 4096x2160 | allowed fps: 24fps)
			- "res_uhd_4k" (4K UHD 3840x2160 | allowed fps: 24/25/30fps)
			- "res_1080p" (FHD 1920x1080 | allowed fps: 24/25/30/48/50/60fps)
		framerate : str, optional
			the photo file format (default = "fps_24")
			- "fps_X" (24/25/30/48/50/60)
		hyperlapse : str, optional
			The ratio at which to drop frames (default = "ratio_15")
			only used if mode is "hyperlapse"
			- "ratio_X" (15/30/60/120/240)
		'''
		
		self.drone(set_camera_mode(cam_id=0, value="recording")).wait()
		self.drone(set_recording_mode(
			cam_id = 0,
			mode = mode,
			resolution = resolution,
			framerate = framerate,
			hyperlapse = hyperlapse,
		)).wait()
		self.camera_mode = "recording"
		print("< Recording Mode >")

	def start_recording(self):
		'''
		Starts a recording
		'''
		if (self.camera_mode != "recording"):
			setup_recording()
		self.media_saved = self.drone(recording_progress(result="stopped", _policy="wait"))
		self.drone(start_recording(cam_id=0)).wait()
		print ("< Recording Started >")

	def stop_recording(self):
		'''
		Stops current recording
		'''
		self.drone(stop_recording(cam_id=0)).wait()
		self.media_saved.wait()	
		path = self.download_last_media()
		print("< Recording Stopped >")
		return path

	# << Photo & Recording Download Methods >>
	def download_last_media(self):
		'''
		Downloads the last media taken
		
		Return
		----------
		download_path : str
			the location of the downloaded image
		'''	
		# Get Photo Media ID
		media_id = self.media_saved.received_events().last().args["media_id"]
		media_info_response = requests.get(self.drone_media_api_url + media_id)
		media_info_response.raise_for_status()
	
		# Download the photo
		for resource in media_info_response.json()["resources"]:
			image_response = requests.get(self.drone_url + resource["url"], stream=True)
			download_path = os.path.join(self.download_dir, resource["resource_id"])
			image_response.raise_for_status()

			with open(download_path, "wb") as image_file:
				shutil.copyfileobj(image_response.raw, image_file)
		print("< Media Downloaded >")
		return download_path
	
	# << Stream Methods >>
	def setup_stream(self,
		value = 2,
		record = False,
		yuv_frame_processing = "None",
		yuv_frame_cb = "None",
		h264_frame_cb = "None",
		start_cb = "None",
		end_cb = "None",
		flush_cb = "None",
	):
		'''
		Prepares the drone camera for streaming, and changes camera_mode to "streaming"
		
		Parameters
		----------
		value : int or str, optional
			the streams priority values (default = 2)
			- 0 or "low_latency"
			- 1 or "high_reliability"
			- 2 or "hight_reliability_low_framerate"
		record : bool, optional
			if true the drone sends a recording of the videos (default = False)
		yuv_frame_processing : method, optional
			a callback for live video processing
			default: saves each yuv frame as an image from a frame queue
		yuv_frame_cb : method, optional
			a callack to prepare live video processing
			default: sends each yuv frame to a frame queue
		h264_frame_cb : method, optional
			a callback for h264 frame processing
			default: saves bitrate and framerate information
		start_cb : method, optional
			a callback for the start of the stream
			default: pass
		end_cb : method, optional
			a callback for the stop of the stream
			default: pass
		flush_cb: method, optional
			a callback to flush the frame queue
			default: flushes the frame queue
		'''
		
		yuv_frame_processing, yuv_frame_cb, h264_frame_cb, start_cb, end_cb, flush_cb = self.cb_helper(
			yuv_frame_processing, yuv_frame_cb, h264_frame_cb, start_cb, end_cb, flush_cb
		)	
	
		if h264_frame_cb == self.h264_frame_cb:
			self.h264_frame_stats = []
			self.h264_stats_file = open(os.path.join(self.download_dir, "h264_stats.csv"), "w+")
			self.h264_stats_writer = csv.DictWriter(
			    self.h264_stats_file, ["fps", "bitrate"]
			)
			self.h264_stats_writer.writeheader()

		self.frame_queue = queue.Queue()
		self.processing_thread = threading.Thread(target= yuv_frame_processing)
		self.renderer = None
		self.frame_counter = 0
	
		if self.drone_rtsp_port is not None:
			self.drone.streaming.server_addr = f"{self.drone_ip}:{self.drone_rtsp_port}"

		if record == True:
			self.drone.streaming.set_output_files(
			    video=os.path.join(self.download_dir, "streaming.mp4"),
			    metadata=os.path.join(self.download_dir, "streaming_metadata.json"),
			)

		self.drone.streaming.set_callbacks(
		    raw_cb = yuv_frame_cb,
		    h264_cb = h264_frame_cb,
		    start_cb = start_cb,
		    end_cb = end_cb,
		    flush_raw_cb = flush_cb,
		)
	
		self.drone(set_streaming_mode(cam_id = 0, value = value)).wait()
		self.camera_mode = "streaming"
		print("< Streaming Mode >")
	
	def cb_helper(self, yuv_frame_processing, yuv_frame_cb, h264_frame_cb, start_cb, end_cb, flush_cb):
		if yuv_frame_processing == "None": 
			yuv_frame_processing = self.yuv_frame_processing
		if yuv_frame_cb == "None":
			yuv_frame_cb = self.yuv_frame_cb
		if h264_frame_cb == "None":
			h264_frame_cb = self.h264_frame_cb
		if start_cb == "None":
			start_cb = self.start_cb
		if end_cb == "None":
			end_cb = self.end_cb
		if flush_cb == "None":
			flush_cb = self.flush_cb
		return yuv_frame_processing, yuv_frame_cb, h264_frame_cb, start_cb, end_cb, flush_cb	

	def start_stream(self):
		if (camera_mode != "streaming"):
			setup_streaming()
		self.drone.streaming.start()
		self.renderer = PdrawRenderer(pdraw=self.drone.streaming)
		self.running = True
		self.processing_thread.start()
		print("< Stream Started >")

	def stop_stream(self):
		self.running = False
		self.processing_thread.join()
		if self.renderer is not None:
		    self.renderer.stop()
		assert self.drone.streaming.stop()
		print("< Stream Stopped >")

	def yuv_frame_cb(self, yuv_frame):
		"""
		This function will be called by Olympe for each decoded YUV frame.

		:type yuv_frame: olympe.VideoFrame
		"""
		
		yuv_frame.ref()
		self.frame_queue.put_nowait(yuv_frame)
		
	def yuv_frame_processing(self):
		while self.running:
			try:
				yuv_frame = self.frame_queue.get(timeout=0.1)
				self.frame_counter += 1
				
				# the VideoFrame.info() dictionary contains some useful information
				# such as the video resolution
				info = yuv_frame.info()

				height, width = (  # noqa
				    info["raw"]["frame"]["info"]["height"],
				    info["raw"]["frame"]["info"]["width"],
				)

				# yuv_frame.vmeta() returns a dictionary that contains additional
				# metadata from the drone (GPS coordinates, battery percentage, ...)

				# convert pdraw YUV flag to OpenCV YUV flag
				cv2_cvt_color_flag = {
					olympe.VDEF_I420: cv2.COLOR_YUV2BGR_I420,
					olympe.VDEF_NV12: cv2.COLOR_YUV2BGR_NV12,
				}[yuv_frame.format()]

				cv2frame = cv2.cvtColor(yuv_frame.as_ndarray(), cv2_cvt_color_flag)
				cv2.imwrite(os.path.join(self.download_dir, "test{}.jpg".format(self.frame_counter)), cv2frame)
				
			except queue.Empty:
				continue
		# You should process your frames here and release (unref) them when you're done.
		# Don't hold a reference on your frames for too long to avoid memory leaks and/or memory
		# pool exhaustion.
		yuv_frame.unref()

	def flush_cb(self, stream):
		if stream["vdef_format"] != olympe.VDEF_I420:
			return True
		while not self.frame_queue.empty():
			self.frame_queue.get_nowait().unref()
		return True
	
	def start_cb(self):
		pass
	
	def end_cb(self):
		pass
	
	def h264_frame_cb(self, h264_frame):
		"""
		This function will be called by Olympe for each new h264 frame.

		    :type yuv_frame: olympe.VideoFrame
		"""

		# Get a ctypes pointer and size for this h264 frame
		frame_pointer, frame_size = h264_frame.as_ctypes_pointer()

		# For this example we will just compute some basic video stream stats
		# (bitrate and FPS) but we could choose to resend it over an another
		# interface or to decode it with our preferred hardware decoder..

		# Compute some stats and dump them in a csv file
		info = h264_frame.info()
		frame_ts = info["ntp_raw_timestamp"]
		if not bool(info["is_sync"]):
			while len(self.h264_frame_stats) > 0:
				start_ts, _ = self.h264_frame_stats[0]
				if (start_ts + 1e6) < frame_ts:
					self.h264_frame_stats.pop(0)
				else:
			    		break
			self.h264_frame_stats.append((frame_ts, frame_size))
			h264_fps = len(self.h264_frame_stats)
			h264_bitrate = 8 * sum(map(lambda t: t[1], self.h264_frame_stats))
			self.h264_stats_writer.writerow({"fps": h264_fps, "bitrate": h264_bitrate})
