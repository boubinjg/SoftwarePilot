from AnafiCameraMedia import AnafiCameraMedia
from AnafiCameraControls import AnafiCameraControls

class AnafiCamera:
	'''
	Wrapper for the Parrot Olympe camera methods splitting media and camera controls

	...
	
	Attributes
	----------
	media : AnafiCameraMedia
		the drone camera media method interface
	controls : AnafiCameraControls
		the drone camera controls method interface
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
	
		self.media = AnafiCameraMedia(drone_object, drone_ip, drone_rtsp_port, drone_url, download_dir)
		self.controls = AnafiCameraControls(drone_object)
