import olympe
from olympe.messages.camera import (
	reset_zoom,
	reset_alignment_offsets,
	set_zoom_target,
	set_alignment_offsets,
	alignment_offsets,
)

# << Camera Movementa and Zoom Methods >>
class AnafiCameraControls:
	'''
	Wrapper for the Parrot Olympe camera methods involving camera controls
	
	...
	
	Attributes
	----------
	drone : olympe.Drone
		the drone object
		
	Methods
	-------
	reset_zoom()
		resets the drone's camera zoom
	reset_alignment_offsets()
		resets the drone's camera alignment offset
	set_zoom(target, control_mode)
		sets the drone's camera zoom, must call reset_zoom before being called again
	set_alignment_offsets(yaw, pitch, roll, wait)
		sets the drone's camera alignment offset
	'''

	def __init__(self, drone_object):
		'''
		Parameters
		----------
		drone_object : olympe.Drone
			the drone object
		'''
		
		self.drone = drone_object
	
	def reset_zoom(self):
		'''
		resets the drone's camera zoom
		'''
		
		self.drone(reset_zoom(cam_id = 0)).wait()
	
	def reset_alignment_offsets(self):
		'''
		resets the drone's camera alignment offset
		'''
		
		self.drone(reset_alignment_offsets(cam_id = 0)).wait()
	
	def set_zoom(self, target, control_mode = "level"):
		'''
		sets the drone's camera zoom, must call reset_zoom before being called again
		
		Parameters
		----------
		target : float
			the level or velocity of zoom
		control_mode : str, optional
			the target interpretation (default = "level")
			- "level"/"velocity"
		'''
		reset_zoom()
		self.drone(set_zoom_target(cam_id = 0, control_mode = control_mode, target = target)).wait()
	
	def set_alignment_offsets(self, yaw, pitch, roll, wait = False):
		'''
		sets the drone's camera alignment offset
		
		Parameters
		----------
		yaw : float
			the yaw camera offset
		pitch : float
			the pitch camera offset
		roll : float
			the roll camera offset
		wait : bool, optional
			if true waits for completion before sending the next instruction (default = False)
		'''
		
		reset_alignment_offsets()
		if wait == False:
			self.drone(set_alignment_offsets(cam_id = 0, yaw = yaw, pitch = pitch, roll = roll)).wait()
		else:
			self.drone(
				set_alignment_offsets(cam_id = 0, yaw = yaw, pitch = pitch, roll = roll)
				>> alignment_offsets(cam_id = 0, 
					current_yaw = yaw,
					current_pitch = pitch,
					current_roll = roll
				)
			).wait().success()
