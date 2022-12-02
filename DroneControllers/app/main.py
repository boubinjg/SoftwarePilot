from fastapi import File, UploadFile, FastAPI
import uvicorn
from AnafiController import AnafiController

HOST = "0.0.0.0" # REPLACE: IP for the URL
PORT = 8000 # REPLACE: Port for the URL
INPUT_SHAPE = "None" # REPLACE: the expected input shape
RESPONSE_KEYS = ["key"] # REPLACE: keys to be included in the response, must match response_values
PATH = "None" # REPLACE: Path to download images

app = FastAPI()
drone = None

@app.get("/")
async def get_port():
	return {"ip": HOST, "port": PORT, "input shape": INPUT_SHAPE, "output keys": RESPONSE_KEYS, "download path": PATH}

@app.get("/set/")
async def setDrone(connection_type: int = 1, download_dir: str = "None"):
	global drone
	try:
		drone = AnafiController(connection_type, download_dir)
		return {"result" : "drone set"}
	except:
		return {"result" : "failed"}

@app.get("/connect/")
async def connect():
	global drone
	if (drone != None):
	#	try:
		drone.connect()
		return {"result" : "connected"}
	#	except:
	#		return {"result" : "failed"}
	return {"result" : "no drone"}

@app.get("/disconnect/")
async def disconnect():
	global drone
	if (drone != None):
		try:
			drone.disconnect()
			return {"result" : "disconnected"}
		except:
			return {"result" : "failed"}
	return {"result" : "no drone"}

@app.get("/takeoff/")
async def takeoff():
	global drone
	if (drone != None):
		try:
			drone.piloting.takeoff()
			return {"result" : "taken off"}
		except:
			return {"result" : "failed"}
	return {"result" : "no drone"}
	
@app.get("/land/")
async def land():
	global drone
	if (drone != None):
		try:
			drone.piloting.land()
			return {"result" : "landed"}
		except:
			return {"result" : "failed"}
	return {"result" : "no drone"}

@app.get("/move/distance/")
async def move_by(x:float, y:float, z:float, angle:float, wait:bool = False):
	global drone
	if (drone != None):
		try:
			drone.piloting.move_by(x, y, z, wait)
			return {"result" : "move by succeeded"}
		except:
			return {"result" : "failed"}
	return {"result" : "no drone"}

@app.get("/move/waypoint/")
async def move_to(lat:float, lon:float, alt:float, orientation_mode:str = "None", heading:float = 0, wait:bool = False):
	global drone
	if (drone != None):
		try:
			drone.piloting.move_to(lat, lon, alt, orientation_mode, heading, wait)
			return {"result" : "move to succeeded"}
		except:
			return {"result" : "failed"}
	return {"result" : "no drone"}

@app.get("/move/distance/cancel/")
async def cancel_move_by():
	global drone
	if (drone != None):
		try:
			drone.piloting.cancel_move_by()
			return {"result" : "move by canceled"}
		except:
			return {"result" : "failed"}
	return {"result" : "no drone"}

@app.get("/move/waypoint/cancel/")
async def cancel_move_to():
	global drone
	if (drone != None):
		try:
			drone.piloting.cancel_move_to()
			return {"result" : "move to canceled"}
		except:
			return {"result" : "failed"}
	return {"result" : "no drone"}
	
@app.get("/setup/photo/")
async def setup_photo(
	mode:str = "single",
	format:str = "rectilinear",
	file_format:str = "dng",
	burst:str = "burst_14_over_1s",
	bracketing:str = "preset_1ev",
	capture_interval:float = 0.1
):
	global drone
	if (drone != None):
		try:
			drone.camera.media.setup_photo(mode, format, file_format, burst, bracketing, capture_interval)
			return {"result" : "photos setup"}
		except:
			return {"result" : "failed"}
	return {"result" : "no drone"}

@app.get("/setup/recording/")
async def setup_recording(
	mode:str = "standard",
	resolution:str = "res_dci_4k",
	framerate:str = "fps_24",
	hyperlapse:str = "ratio_15",
):
	global drone
	if (drone != None):
		try:
			drone.camera.media.setup_recording(mode, resolution, framerate, hyperlapse)
			return {"result" : "recording setup"}
		except:
			return {"result" : "failed"}
	return {"result" : "no drone"}

@app.get("/setup/streaming/")
async def setup_streaming(
	value:int = 2,
	record:bool = False,
	yuv_frame_processing:str = "None",
	yuv_frame_cb:str = "None",
	h264_frame_cb:str = "None",
	start_cb:str = "None",
	end_cb:str = "None",
	flush_cb:str = "None"
):
	global drone
	if (drone != None):
		try:
			drone.camera.media.setup_streaming(
				value, record, yuv_frame_processing, yuv_frame_cb, 
				h264_frame_cb, start_cb, end_cb, flush_cb
			)
			return {"result" : "streaming setup"}
		except:
			return {"result" : "failed"}
	return {"result" : "no drone"}
	
@app.get("/take/photo/")
async def take_photo():
	global drone
	if (drone != None):
		#try:
		drone.camera.media.take_photo()
		return {"result" : "photo taken"}
		#except:
		#	return {"result" : "failed"}
	return {"result" : "no drone"}
	
@app.get("/take/recording/start")
async def start_recording():
	global drone
	if (drone != None):
		try:
			drone.camera.media.start_recording()
			return {"result" : "recording started"}
		except:
			return {"result" : "failed"}
	return {"result" : "no drone"}
	
@app.get("/take/recording/stop")
async def stop_recording():
	global drone
	if (drone != None):
		try:
			drone.camera.media.stop_recording()
			return {"result" : "recording stopped"}
		except:
			return {"result" : "failed"}
	return {"result" : "no drone"}
	
@app.get("/take/streaming/start")
async def start_streaming():
	global drone
	if (drone != None):
		try:
			drone.camera.media.start_streaming()
			return {"result" : "streaming started"}
		except:
			return {"result" : "failed"}
	return {"result" : "no drone"}

@app.get("/download/")
async def download():
	global drone
	if (drone != None):
		try:
			drone.camera.media.download_last_media()
			return {"result" : "downloaded"}
		except:
			return {"result" : "failed"}
	return {"result" : "no drone"}

if __name__ == '__main__':
    uvicorn.run(app, host=HOST, port=PORT)
