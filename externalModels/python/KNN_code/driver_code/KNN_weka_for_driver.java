/*
*********************************************************************************************************
*													*
*    @Naveen T R											*
*    June 7th 2018											*
*    Implementation of KNN (K nearest neighbors) to find the k-nearest neighbors to choose from.	*
*    													*	
*    In our context near would mean nearest matching features as to current position's features.	*
*    													*
*    Steps followed in this process are:-								*
*    1. Compile featureSet which would be for every image in every cube.				*
*    2. Drone takes off and an image is taken.								*
*    3. Read-in the image taken and extract all the feature values.					*
*    4. Store the possible Hop Movement and utility value in a HashMap for current position		*
*    5. Then:												*
*          do 												*
*          {												*
*            Find the K-nearest neighbors								*
*            Average their utility									*
*           }												*	
*    6. Check the boundary being satisfied and Return the position where max of utility			*
*	could be achieved.										*
*													*
*													*
*********************************************************************************************************/

//Import the usual Java built-in libraries for io operation.
import java.util.*;
import java.lang.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import java.util.Collections; 
import java.util.Comparator;
import java.util.HashMap; 
import java.util.LinkedHashMap; 
import java.util.Map; 
import static java.util.stream.Collectors.*; 
import static java.util.Map.Entry.*;


//Import libraries for file-handling
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
 
//Import librarries for weka classification
import weka.classifiers.*;
import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.core.*;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.neighboursearch.*;
import weka.core.SparseInstance;
import weka.core.DenseInstance;

public class KNN_weka_for_driver
{
	 //Declaration of global variables.
	// List of Possibilities stored in this variable.
	static  List<List<Integer>> possibilities = new ArrayList<>();
	static int mRight = 1, mLeft = 1, mDown = 1, mUp = 1, mFwd = 1, mBwd = 1;

	// This is a global Map which stores possible movements, Utility value.
	//Create a Map of <String, Double> pair to hold the movement and Double value for the utility.
	static Map<String, Double> map = new HashMap<>();

	//This Map is used for choosing the best action to be taken.
	static Map<String, Double> sorted_actions = new LinkedHashMap<>();

	

	 /* From all the possible best actions to take and based on fixed boundary, an appropriate best action is returned.
	 	Example: If move_right = 0.8666 and move_down = 0.7667:- 
	 	- If the drone is already in rightmost position, then move_down action would be taken.
	 	- If drone is not in leftmost position, move right is taken.
	 */
	 	
	 public static String choose_best_action(Map<String, Double> actions, Map<String, Double> current_feature)
	 {

	 	//Use another map to store gain in utility value and next sort them in descending order.
	 	Map<String, Double> gain =  new HashMap<>();

	 	for(Map.Entry<String, Double> entry : actions.entrySet())
	 	{

	 		String key = entry.getKey();
	 		//System.out.println("Key of map="+key);
	 		Double val = entry.getValue();
	 		//System.out.println("Val of map="+val);

	 		gain.put(key, val - current_feature.get(key));
	 	}

	 	// Now pass the gain to be sorted in descending order.
	 	//Sort the Map of actions in descending order. (Using Java8 Lambda expression)
	 	sorted_actions = gain
	 	    .entrySet()
	 	    .stream()
	 	    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
	 	    .collect(
	 	    	toMap(Map.Entry::getKey, Map.Entry::getValue, (e1,e2) -> e2,
	 	    	LinkedHashMap::new));

	 	//System.out.println("Map After sorting in descending order:"+sorted_actions);



	 	//Create an iteartor and check if the enrty is permitted action to take.
	 	for(Map.Entry<String, Double> entry: sorted_actions.entrySet())
	 	{
	 		//Get the key from the "sorted_actions" Map.
	 		String key = entry.getKey();

	 		//Get the value from the "sorted_actions" Map.
	 		Double val = entry.getValue();


	 		if(key == "Left")
	 		{
	 		if ((mLeft-1>=0) && (mRight+1<=2))
	 		{
	 			//Decrement the remaining moveLeft value.
	 			mLeft -= 1;

	 			//Increment the remaining moveRight Value.
	 			mRight += 1;

	 			return "Left";
	 		}

	 		//If that action is not permitted then just remove that from the map.
	 		else
	 		{
	 			sorted_actions.remove(key);
	 		}
	 		}

	 		else if(key == "Right")
	 		{
	 		if ((mRight-1>=0) && (mLeft+1<=2))
	 		{
	 			//Decrement the remaining moveLeft value.
	 			mRight -= 1;

	 			//Increment the remaining moveRight Value.
	 			mLeft += 1;

	 			return "Right";
	 		}

	 		//If that action is not permitted then just remove that from the map.
	 		else
	 		{
	 			sorted_actions.remove(key);
	 		}
	 		}

	 		else if(key == "Up")
	 		{
	 		if ((mUp-1>=0) && (mDown+1<=2))
	 		{
	 			//Decrement the remaining moveLeft value.
	 			mUp -= 1;

	 			//Increment the remaining moveRight Value.
	 			mDown += 1;

	 			return "Up";
	 		}

	 		//If that action is not permitted then just remove that from the map.
	 		else
	 		{
	 			sorted_actions.remove(key);
	 		}
	 		}

	 		else if(key == "Down")
	 		{
	 		if ((mDown-1>=0) && (mUp+1<=2))
	 		{
	 			//Decrement the remaining moveLeft value.
	 			mDown -= 1;

	 			//Increment the remaining moveRight Value.
	 			mUp += 1;

	 			return "Down";
	 		}

	 		//If that action is not permitted then just remove that from the map.
	 		else
	 		{
	 			sorted_actions.remove(key);
	 		}
	 		}

	 		else if(key == "Forw")
	 		{
	 		if ((mFwd-1>=0) && (mBwd+1<=2))
	 		{
	 			//Decrement the remaining moveLeft value.
	 			mFwd -= 1;

	 			//Increment the remaining moveRight Value.
	 			mBwd += 1;

	 			return "Forw";
	 		}

	 		//If that action is not permitted then just remove that from the map.
	 		else
	 		{
	 			sorted_actions.remove(key);
	 		}
	 		}

	 		else if(key == "Backwd")
	 		{
	 		if ((mBwd-1>=0) && (mFwd+1<=2))
	 		{
	 			//Decrement the remaining moveLeft value.
	 			mBwd -= 1;

	 			//Increment the remaining moveRight Value.
	 			mFwd += 1;

	 			return "Backwd";
	 		}

	 		//If that action is not permitted then just remove that from the map.
	 		else
	 		{
	 			sorted_actions.remove(key);
	 		}
	 		}

	 	}


	 	// If all the actions aren't possible, then just land the drone.
	 	if(sorted_actions.isEmpty())
	 	{
	 		System.out.println("No possible further move. Drone landing down!");
	 		return "Land";
	 	}

	 	return "UnknownError";

	 }
    
   /* This function uses weka's K-Nearest neighbors to find the K-Nearest neighbor values and return the average of utilities of those values.
      The X-Y-Z co-ordiantes of nearest one hop is passed as the input and we will get average utility value (A double value)
    */
    public static Instances KNN_Instances(List<Double> points, String file_name) throws Exception
    {

    	Set<Integer> reqd = new HashSet<>();

    	// Now add the required attributes indices to hashSet.
    	// Later all indices except this would be set to zero.
    	reqd.add(0); // Width = 0
    	reqd.add(1); // height = 1
    	reqd.add(2); // centerx =
    	reqd.add(3); // centery =
    	reqd.add(19); // Latitude =
    	reqd.add(20); // Longitutude =
    	reqd.add(21); // brightnessOfMask
    	reqd.add(22); // brightnessOfRest
    	reqd.add(23); // brightnessTotal
    	reqd.add(28); // exposureTime
    	reqd.add(29); // gimbalPosition
    	reqd.add(32); // SaturationOfMaskRed
    	reqd.add(33); // SaturationOfMaskGreen
    	reqd.add(34); // SaturationOfMaskBlue
    	reqd.add(35); // SaturationOfRestRed
    	reqd.add(36); // SaturationOfRestGreen
    	reqd.add(37); // SaturationOfRestBlue
    	reqd.add(50); // SaturationTotalRed
    	reqd.add(51); // SaturationTotalGreen
    	reqd.add(52); // SaturationTotalBlue
    	reqd.add(53); // time

    	double[] targetArray = new double[points.size()];
		for(int i=0;i< targetArray.length;i++)
		{
			targetArray[i] = points.get(i);

		}

		//Instance curr = new DenseInstance(1,list.toDoubleArray());
		// Create an Instance from ArrayList of values provided at current position.
		Instance curr = new DenseInstance(1,targetArray);
		//System.out.println("Instance vals = "+curr.toStringNoWeight());

		// Instantiate new data object.
		Instances data = new Instances(new BufferedReader(new FileReader(file_name)));
		data.setClassIndex(data.numAttributes() - 1);


		//LOOP from i = 0 to 63 and set the required weights to 0.
		for(int i=0;i<64;i++)
		{
			if(!reqd.contains(i))
			{
				//System.out.println("val="+i);
				//Sets the sttribute weight to zero.
				data.setAttributeWeight(i,0);
			}
		}

		//Instance in = data.instance(3);
		//Attribute attr = in.

		//Check if all the attributes weights are identical.
		System.out.println("Is all attributes weights identical="+data.allAttributeWeightsIdentical());
		//Currently creating current instance as the 2nd entry from the input file.
		//Instance inst = data.instance(1);

		// Instantiate 'knn' object.
		NearestNeighbourSearch knn = new LinearNNSearch(data);

		// KNN is run based on current psotion's feature values and offline dataset.
    	Instances ins = knn.kNearestNeighbours(curr,3); // Call weka's kNearestNeighbours method.

    	//System.out.println("3 NN === " +ins.toString());

    	//Return the result stored in ins after running weka's kNN algorithm.
    	return ins;

	}	

    /* Based on the instances returned from KNN algorithm, convert the instnaces to suitable HashMap of required utilities and values.
       This Map would be later used to calculate the most beneficial move
       */

	public static void convert_to_map_average(Instances inputVal)
	{


		//Get the count of number of instances.
		int count = inputVal.numInstances();

		//The value from 32 to 37 holds the required utility values.
		for(Instance data:inputVal)
		{

				if(!map.containsKey("Left"))
				{
					map.put("Left",data.value(53)); //previous 32
				}
				else
				{
					map.replace("Left",map.get("Left")+data.value(53));
				}

				if(!map.containsKey("Right"))
				{
					map.put("Right",data.value(54));
				}
				else
				{
					map.replace("Right",map.get("Right")+data.value(54));
				}

				if(!map.containsKey("Up"))
				{
					map.put("Up",data.value(55));
				}
				else
				{
					map.replace("Up",map.get("Up")+data.value(55));
				}

				if(!map.containsKey("Down"))
				{
					map.put("Down",data.value(56));
				}
				else
				{
					map.replace("Down",map.get("Down")+data.value(56));
				}

				if(!map.containsKey("Forw"))
				{
					map.put("Forw",data.value(57));
				}
				else
				{
					map.replace("Forw",map.get("Forw")+data.value(57));
				}

				if(!map.containsKey("Backwd"))
				{
					map.put("Backwd",data.value(58));
				}
				else
				{
					map.replace("Backwd",map.get("Backwd")+data.value(58));
				}


			}


		// Now divide the sums calculated from the previous step. Just iterate over each key,value pair and divide each key by
		// the number of instnaces used (This value is stored in "count" variable.

		for(Map.Entry<String, Double> entry:map.entrySet())
		{
			String key = entry.getKey();
			Double val = entry.getValue();

			val = val/count; // Taking the average/mean of values calculated.

			//Replace the map's entry with average value that was computed.
			map.replace(key,val);

		}

	}


	//Main function where the invokation of functions happen.
    public static void main(String args[])
    {
	
	try{

        System.out.println("Inside main function");
        System.out.println("Drone is taking off and an image is captured");
		
    	// Extract current feature value and sav eit in a list.
		/*List<Integer> list = new ArrayList<>();

		// I am using dummy data as of now.
		list.add(1);
		list.add(2);
		list.add(0);*/

		//Below would be a constructed dummy list as of now.
		List<Double> list = new ArrayList<>();
		
		//Populate the dummy list with appropriate dummy values

		list.add(0.021673387096774195);
		list.add(0.02889784946236559);
		list.add(0.6827116935483871);
		list.add(0.03931451612903226);
		list.add(74.0);
		list.add(2752.0);
		list.add(160.0);
		list.add(2666.0);
		list.add(0.023185483871);
		list.add(0.0309139784946);
		list.add(0.684223790323);
		list.add(0.039314516129);
		list.add(0.0448588709677);
		list.add(0.059811827957);
		list.add(0.562247983871);
		list.add(0.213709677419);
		list.add(1.0);
		list.add(1.0);
		list.add(1.0);
		list.add(0.0);
		list.add(0.0);
		list.add(0.17296470204250344);
		list.add(0.2849698709368885);
		list.add(0.28644712472777156);
		list.add(0.16872228122616917);
		list.add(0.2848825569615346);
		list.add(0.21983051938725473);
		list.add(0.2818385458789189);
		list.add(0.03333333333333333);
		list.add(0.5);
		list.add(3968.0);
		list.add(2976.0);
		list.add(0.16804738120234572);
		list.add(0.17224519878259578);
		list.add(0.19591777219270617);
		list.add(0.2808308001117557);
		list.add(0.2881399708706518);
		list.add(0.28992947400273356);
		list.add(0.1643278105192928);
		list.add(0.1678699173431187);
		list.add(0.19123206938730125);
		list.add(0.2808436756391706);
		list.add(0.2881535983160713);
		list.add(0.2899413435516942);
		list.add(0.2504532503570801);
		list.add(0.20678984382542312);
		list.add(0.2319901676110414);
		list.add(0.28084169721669044);
		list.add(0.28828604605283936);
		list.add(0.29002630938550983);
		list.add(0.2807601622462092);
		list.add(0.28806738432018664);
		list.add(0.28987059313162267);
		list.add(0.5709143518518518);
		list.add(0.35496717531343064);
		list.add(0.3405013819169034);
		list.add(0.0);
		list.add(0.40144650904747925);
		list.add(0.43575789496511025);
		list.add(0.0);
		list.add(0.4060991274079092);
		list.add(0.39743689141174576);
		list.add(0.35496717531343064);
		list.add(0.0);

		System.out.println("Size of list="+list.size());

		/*double[] targetArray = new double[list.size()];
		for(int i=0;i< targetArray.length;i++)
		{
			targetArray[i] = list.get(i);

		}

		//Instance curr = new DenseInstance(1,list.toDoubleArray());
		Instance curr = new DenseInstance(1,targetArray);
		System.out.println("Instance vals = "+curr.toStringNoWeight());*/

		Instances result = KNN_Instances(list,"Final_list"); //This is working fine.

		// The result of KNN would be stored in "result".
		// This needs to be processed to extract utility and then select utility with max mean value(average).
		result.setClassIndex(result.numAttributes() - 1);

		//System.out.println("Size="+result.numInstances());
		//Extract the first entry, second and third and print them.
		for(int i=0;i<3;i++)
		{
			Instance cur = result.instance(i);
			//System.out.println("Value of "+i+" instance = "+cur.toString());

		}	

		convert_to_map_average(result);

		// Creating an iterator and cheking that the map values are populated correctly.
		//System.out.println("After the map construction and averaging:");
		for(Map.Entry<String, Double> entry: map.entrySet())
		{
			String key = entry.getKey();
			Double val = entry.getValue();

			//System.out.println("Key="+key+"Value="+val);
		}

		//Consider this as Map with utility values at current position.
		// Right now this is hardcoded. It will be populated with actual values when feature extraction 
		// driver from current position is up and running.

		Map<String, Double> current = new HashMap<>();
		current.put("Backwd",0.2);
		current.put("Forw",0.4);
		current.put("Down",0.5);
		current.put("Up",0.3);
		current.put("Left",0.4);
		current.put("Right",0.3);


		String best_act = choose_best_action(map, current);

		System.out.println("The Best possible action to take is: "+best_act);
	}

	// If there's any exception, catch it and print the stack trace.
    catch(Exception e){
         e.printStackTrace();
     }

	}
}

