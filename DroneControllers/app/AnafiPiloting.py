import olympe
from olympe.messages.ardrone3.Piloting import (
	TakeOff,
	Landing,
	moveBy,
	moveTo,
	CancelMoveTo,
	CancelMoveBy,
)
from olympe.messages.ardrone3.PilotingState import (
	FlyingStateChanged,
	PositionChanged,
	moveToChanged,	
)

class AnafiPiloting:
	'''
	Wrapper for the Parrot Olympe flight control methods
	
	...
	
	Attributes
	----------
	drone : olympe.Drone
		the drone object
		
	Methods
	-------
	takeoff()
		initiates drone takeoff
	land()
		initiates drone landing
	move_by(x, y, z, angle, wait)
		moves the drone a given number of meters or rotates it to a set angle
	move_to(lat, lon, alt, orientation_mode, heading, wait)
		moves the drone to given waypoint or rotates it to a set angle from north
	cancel_move_by()
		cancels move_by order
	cancel_move_to
		cancels move_to order
	'''
	
	def __init__(self, drone_object):
		'''
		Parameters
		----------
		drone_object : olympe.Drone
			the drone object
		'''
		
		self.drone = drone_object	

	def takeoff(self):
		'''
		initiates drone takeoff
		'''
		
		assert self.drone(
			TakeOff()
			>> FlyingStateChanged(state = "hovering", _timeout=5)
		).wait().success()
	
	def land(self):
		'''
		Initiates drone landing
		'''
		
		assert self.drone(Landing()).wait().success()

	def move_by(self, x, y, z, angle, wait = False):
		'''
		moves the drone a given number of meters or rotates it to a set angle
		
		Parameters
		----------
		x : float
			the movement in the x axis, forwards and backwards, in meters
		y : float
			the movement in the y axis, left and right, in meters
		z : float
			the movement in the z axis, up and down, in meters
		angle : float
			the rotation in radians
		wait : bool, optional
			if true waits for completion before sending the next instruction (default = False)
		'''
		
		if wait == False:
			self.drone(moveBy(x, y, z, angle))
		else:		
			self.drone(moveBy(x, y, z, angle) >> FlyingStateChanged(state = "hovering", _timeout=5)).wait()
	
	def move_to(self, lat, lon, alt, orientation_mode = "NONE", heading = 0, wait = False):
		'''
		moves the drone to given waypoint or rotates it to a set angle from north
		
		Parameters
		----------
		lat : float
			the latitude to travel to
		lon : float
			the longitude to travel to
		alt : float
			the altitude to travel to
		orientation_mode : str, optional
			the orientation mode (default = "NONE")
			- "NONE"
			- "TO_TARGET" (looks at the direction of the given location)
			- "HEADING_START" (Changes orientation before travelling)
			- "HEADING_DURING" (Changes orientation during travel)
		heading : float, optional
			the target orientation dictated by degrees from north (default = 0)
			only used if orientation_mode is "HEADING_START" or "HEADING_DURING"
		wait : bool, optional
			if true waits for completion before sending the next instruction (default = False)
		'''
		
		if wait == False:
			self.drone(
				moveTo(
					latitude = lat,
					longitude = lon,
					altitude = alt,
					orientation_mode = orientation_mode,
					heading = heading,
				)
			)
		else:		
			self.drone(
				moveTo(
					latitude = lat,
					longitude = lon,
					altitude = alt,
					orientation_mode = orientation_mode,
					heading = heading,
				)
				>> moveToChanged(status = "DONE").wait()
			)
		
	def cancel_move_by(self):
		'''
		cancels move_by order
		'''
		
		assert self.drone(CancelMoveBy()).wait()

	def cancel_move_to(self):
		'''
		cancels move_to order
		'''
		
		assert self.drone(CancelMoveTo()).wait()
