import olympe
from olympe.messages.rth import ( 
	set_preferred_home_type,
	set_custom_location,
	set_auto_trigger_mode,
	set_delay,
	set_ending_behavior,
	set_ending_hovering_altitude,
	return_to_home,
	abort,
	cancel_auto_trigger,
)

class AnafiRTH:
	'''
	Wrapper for the Parrot Olympe Return To Home (RTH) methods
	
	...
	
	Attributes
	----------
	drone : olympe.Drone
		the drone object
		
	Methods
	-------
	setup_rth(home_type, gps_coordinates, auto_trigger, delay, ending_behavior, ending_hovering_altitude)
		Setup rth features
	return_to_home()
		Returns the drone to rth location
	abort_return_to_home()
		Stops rth call
	cancel_auto_trigger()
		Cancels auto trigger when connection is lost
	'''
	
	def __init__(self, drone_object):
		'''
		Parameters
		----------
		drone : olympe.Drone
			the drone object
		'''
		
		self.drone = drone_object
			
	def setup_rth(self, 
		home_type = "takeoff",
		gps_coordinates = "None",
		auto_trigger = "on",
		delay = 5,
		ending_behavior = "landing",
		ending_hovering_altitude = 2
	):
		'''
		Setup rth features
		
		Parameters
		----------
		home_type : str, optional
			the target location for rth
			- "takeoff"/"pilot"/"custom"
		gps_coordinates : str, optional
			the gps coordinates for a custom rth location
			must be entered in the format longitude,latitude,altitude (comma separated no spaces)
			is only used when the home_type is "custom"
		auto_trigger : str, optional
			If auto_trigger is "on", rth will be automatically triggerred when connection to the drone is lost
			- "on"/"off"
		delay : int, optional
			The delay before rth will be automatically triggered upon connection lost
			is only used if auto_trigger is "on"
		ending_behavior : str, optional
			the behavior after reaching home
			- "landing"/"hovering"
		ending_hovering_altitude : int, optional
			 the altitude at which to hover over the ground once home is reached
			 the value is interpreted as meters above ground
			 is only used if ending_behavior is "hovering"
		'''
	
		self.drone(set_preferred_home_type(home_type)).wait()
		if home_type == "custom":
			gps_coordinates = gps_coordinates.split(',')
			self.drone(set_custom_location(gps_coordinates[0], gps_coordinates[1], gps_coordinates[2])).wait()
		self.drone(set_auto_trigger_mode(auto_trigger)).wait()
		self.drone(set_delay(delay)).wait()
		self.drone(set_ending_behavior(ending_behavior)).wait()
		if ending_behavior == "hovering":
			self.drone(set_ending_hovering_altitude(ending_hovering_altitude)).wait()

	def return_to_home(self):
		'''
		Returns the drone to rth location
		'''
		
		self.drone(return_to_home()).wait()

	def abort_return_to_home(self):
		'''
		Stops rth call
		'''
		
		self.drone(abort()).wait()

	def cancel_auto_trigger(self):
		'''
		Cancels auto trigger when connection is lost
		'''
		
		self.drone(cancel_auto_trigger).wait()
		
		
		
