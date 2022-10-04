# ![SoftwarePilot Logo](http://jaysonboubin.com/images/sp.png)

# SoftwarePilot v1.0

SoftwarePilot is an open source middleware and API that supports aerial applications.
SoftwarePilot allows users to connect consumer DJI drones to 
programmable Java routines that provide access to the drones flight controller, camera, and navigation system as well as computer vision and deep learning software packages
like OpenCV, DLIB, and Tensorflow.

SoftwarePilot comes with a dockerfile and installation scripts for all requisite
software, and an Android-x86 VM to communicate with DJI drones from most systems.

SoftwarePilot provides sample routines and demos ranging from simple flight control applications to fully autonomous aerial systems. A simulation framework for fully autonomous aerial systems that use SoftwarePilot is also provided. The simulation framework can be used to aid development and profiling of complicated aerial systems.

## Getting Started

Please review our comprehensive user guide [here](https://reroutlab.org/softwarepilot/SoftwarePilot_User_Guide_1-0.pdf)
for installation instructions, and our development guide [here](https://reroutlab.org/softwarepilot/SoftwarePilot_Development_Guide_1-0.pdf) to learn about adding to SoftwarePilot.

### Prerequisits

The goal of SoftwarePilot is to be as accessible as possible to anyone with a drone. Therefore, it only requires a basic laptop with the following specifications:

* At least 4GB of ram
* A 2GHz processor
* WiFi capabilities
* 10 GB of available disk space
* Linux, Windows, or MacOS

Software prerequisites include:

* [Docker](http://www.docker.com) 
* [VirtualBox](http://www.virtualbox.org)
* The SoftwarePilot [Virtual Machine](http://www.reroutlab.org/softwarepilot/SoftwarePilot.ova)

## Built With

* [DJI-SDK](http://developer.dji.com) - To interact with DJI drones
* [Java](http://java.com) - To write the API between DJI drones and our programmable routines
* [Python](http://python.org) - For machine learning routines and simulation
* [Docker](http://docker.com) - For portability
* [VirtualBox](http://virtualbox.org) - To run our virtual machine
* [Tensorflow](http://tensorflow.org) - For custom deep models
* [Dlib](http://dlib.net) - For facial recognition
* [OpenCV](http://opencv.org) - For image processing
* [Android-x86](http://android-x86.org) - To execute DJI-SDK apps on most hardware
* [Android](http://android.com) - To use the DJI-SDK

## Authors

* **[Jayson Boubin](http://jaysonboubin.com)** - *Lead Developer*
* **[Christopher Stewart](http://web.cse.ohio-state.edu/~stewart.962/)**
* **[Shiqi Zhang](https://github.com/shiqi7)**
* **[Naveen Tumkur Ramesh Babu](https://naveentr.com/)**
* **[Zichen Zhang](https://github.com/Winchester1896)**

See also the list of [contributors](https://github.com/boubinjg/SoftwarePilot/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [License](LICENSE) file for details


