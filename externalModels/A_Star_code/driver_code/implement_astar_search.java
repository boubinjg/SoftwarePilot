/*
   @Author Naveen T R
   @ Date 06/30/2018
   @ Version 1.0.0

   An Algorithm to implement A* Search for Space Exploration in order to find the Best Selfie.
   The entire map is created offline based on k-nn values and is continuously calculated after miving to every hop.
   Loop until convergence. The weights of edges can be constant or can be energy values.

   The conception of A* changes in our context:

   f(n) = g(n) + h(n)

   where g(n) is the cost to goal (Best Selfie), which should be minimum value
   		 h(n) is the heuristics (i.e Utility values) which can be obtained from KNN.


   while(!convergence)
   {
	compute g(n) and h(n)
	Keep Minimizing f(n)
   }

   return the computed f(n) value.

   Steps followed are:-

   1. Capture the image at current position and extract the features and current utility value.
   2. From this use k-nn to find the position with highest utility and construct an offline map with utlity values.
   3. Choose the best hop and recompute the offline map again.
   4. Loop until convergence.

   */

   // String to record the current position.
   // i.e 1_1_1 indicates starting position and
   // every L/R changes first value, every U/D changes second value
   // and every F/B value changes third value.
   import java.util.*;
   import java.io.*;
   import java.lang.*;
   import java.util.LinkedList;
   import java.util.Queue;

   // weka specific libraries.
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

   public class implement_astar_search
   {


      public static double utility = 0;

      public static Double reqd_threshold = 0.50; //The utility threshold required so that we converge and end the routine.

      //Store all the values which are above threshold utility value. Choose the highest possible utility among the possible options.
      // This will serve as the goal nodes. Choosing the goal can be from user.
      public static Map<String, Double> goal_nodes = new HashMap<String, Double>();

      // This is a global Map which stores possible movements, Utility value.
      //Create a Map of <String, Double> pair to hold the movement and Double value for the utility.
      public static Map<String, Double> map = new HashMap<>();

      //This is a global Map which holds the average feature values caclulated from KNN.
      public static Map<Integer, Double> feature_map = new HashMap<>();

      // Gimbal positions, store the HashMap of Position and value. Format 1_1_1_g30, val = 1.3
      public static Map<String, Double> NodeGimbalVisited = new HashMap<>();

      // States and gimbal, store the features at that position.
      public static Map<String, List<Double>> Features_states = new HashMap<>();

      // Use a Queue for storing positions to be visited. Newly explored positions are added the back and older ones are added at the front.
      public static Queue<String> queue = new LinkedList<>();

      // Use a boolean 3D Array to hold all the visited nodes/states.
      public static boolean[][][] visited = new boolean[2][3][2]; // sets the boolean value to false by default.


         // write a function which returns the highest utility goal node from the calculated possible goal nodes.
         public static String get_goal_nodes()
         {
            Double maxVal = -1000.0; //assign a small enough number such that it is lower than all possible weights
            String maxString = ""; //Position being stored.
            for(Map.Entry<String, Double> entry: goal_nodes.entrySet() )
            {
            //System.out.println(maxVal);
            //System.out.println(entry.getValue());
               // If there's a max entry, update the maxVal and store the position.
               if( entry.getValue() >= maxVal)
               {
                  //System.out.println("MaxVal");
                  maxVal = entry.getValue();
                  maxString = entry.getKey();
                  //System.out.println("maxString="+maxString+"maxVal"+maxVal);
               }

            }

            //System.out.println("Max utility Position:"+maxString+" Value= "+maxVal);
            return maxString;

         }

         public static Set<String> find_one_hop_neighbor_values(String curPos)
         {
            int x = curPos.charAt(0)-'0';
            int y = curPos.charAt(2)-'0';
            int z = curPos.charAt(4)-'0';


            String gimb = curPos.substring(7,9);
            int gimbal = Integer.parseInt(gimb);

            //System.out.println("x="+x+"y="+y+"z="+z+"gimbal="+gimbal);

            // Calculate for all neighboring Gimbal positions.
            visited[x][y][z] = true; //Mark the current position as visited.


            // Calculate for possible one hop movements.
            //System.out.println("The possible one-hop movements are:");
            Set<String> movts = new HashSet<>();

            if(x==0 )
            {
               movts.add("rgh");
               int xX = 1;
            }
            if(x == 1)
            {
               movts.add("lef");
               int xX = 0;
            }
            if(y == 1)
            {
               movts.add("ups");
               movts.add("dwn");
               int yY = 0;
            }
            if(y == 0)
            {
               movts.add("ups");
               int yY = 0;
            }
            if(y == 2)
            {
               movts.add("dwn");
               int yY = 0;
            }
            if(z == 0)
            {
               movts.add("bck");
               int zZ = 0;
            }
            if(z == 1)
            {
               movts.add("fwd");
               int zZ = 0;
            }
            if((x!=0 && x!=1) || (y!=0 && y!=1 && y!=2)|| ( z!=0 && z!=1)) //Check if some Invalid state is reached.
            {
               System.out.println("Invalid number");
            }

            //int len = movts.size();

            //System.out.println("Printing valid moves");

            /*for(String tem:movts)
            {
               System.out.println(tem);
            }*/

            return movts;

         }

         // Given the current position, calculate and return utility values of other three gimbal positions.
         public static Set<String> calculate_3_gimbal_costs(String curPos)
         {
            //Create a HashSet to mark which all gimbal position to visit.
            Set<String> toMoveGimbal = new HashSet<>();
            toMoveGimbal.add("g00");
            toMoveGimbal.add("g15");
            toMoveGimbal.add("g30");

            String curr = curPos.substring(6,9);
            //System.out.println(curr);
            toMoveGimbal.remove(curr);

            //for(String i:toMoveGimbal)
               //System.out.println("Gimbal set vals="+i);

            return toMoveGimbal;

         }


         // Based on the utility at given position construct an offline map which will be helpful for our A* search algorithm.
         public static void construct_offline_map(List<Double> list, String cur_position) throws Exception
         {
            // 1. Get the current position image feature values.
            // 2. Apply Knn and calculate the neighboring utility and feature values.
            // 3. Return the constructed offline_Map i.e a 3D Double Array.

            /*// Choose the closest utility value.
            int x_position = (list.get(16)).intValue();
            int y_position = (list.get(17)).intValue();
            int z_position = (list.get(18)).intValue();

            System.out.println("gimbal from list="+list.get(29));

            String gimbal_position = ((list.get(29) == 0.0)?"g00":(list.get(29) == -0.5)?"g15":"g30");

            String cur_position =  x_position + "_" + y_position + "_" + z_position + "_" + gimbal_position;;

            System.out.println("cur pos="+cur_position);*/
            if(!NodeGimbalVisited.containsKey(cur_position))
            NodeGimbalVisited.put(cur_position,list.get(54)); // Mark the current position as visited and store utility values.

            //cur_position = "0_1_0_g00";

            //System.out.println("Size of list="+list.size());
            Instances result = KNN_Instances(list,"Final_list"); //This is working fine.

            // The result of KNN would be stored in "result".
            // This needs to be processed to extract utility and then select utility with max mean value(average).
            result.setClassIndex(result.numAttributes() - 1);

            //System.out.println("Size="+result.numInstances());
            //Extract the first entry, second and third and print them.
            for(int i=0;i<9;i++)
            {
               Instance cur = result.instance(i);
               System.out.println("Value of "+i+" instance = "+cur.toString());
            }
            System.out.println("______________________________________________");

            convert_to_map_average(result); // Call the method to calculate the average of all the utility values.

            convert_feature_average(result); // Call the method to calculate the average of all feature values.

            List<Double> feature_list = new ArrayList<>(); // List which stores all the feature values.

            // Iterate through the Map and add the entries onto the list.
            for(Map.Entry<Integer, Double> entry: feature_map.entrySet())
            {
               Integer key = entry.getKey();
               Double value = entry.getValue();

               feature_list.add(value);
               //System.out.println("Key="+key+" val = "+value);

            }

            //Create a list of neighbor feature vals.
            List<Double> predicted_neighbor = new ArrayList<>();

            for(Map.Entry<String, Double> entry: map.entrySet())
            {
               Double val = entry.getValue();
               predicted_neighbor.add(val);
            }

            //print out the predicted neighbor values.
            //for(Double d: predicted_neighbor)
            //   System.out.println("Val ="+d );


            //From the current position, populate all the gimbal position values with predicted feature values (i.e the average which was taken).
            Set<String> gimbal_possible = calculate_3_gimbal_costs(cur_position);

            for(String gimbal: gimbal_possible)
            {
               String nextTraverse = cur_position.substring(0,6) + gimbal;
               //System.out.println("predicted gimbal==="+nextTraverse);

               if(!Features_states.containsKey(nextTraverse))
               {
                  Features_states.put(nextTraverse, feature_list);
                  queue.add(nextTraverse);
               }
            }

            // Given the current position, find one hop neighbor values.
            Set<String> possible = find_one_hop_neighbor_values(cur_position);

            // Creating an iterator and cheking that the map values are populated correctly.
            //System.out.println("After the map construction and averaging:");
            String ret = "";
            for(Map.Entry<String, Double> entry: map.entrySet())
            {
               String key = entry.getKey();
               Double val = entry.getValue();
               ret += key+"="+val+" ";
            }
            ret += "Utility="+utility;
            //System.out.println("Utility vals="+ret); // To print out the utility values.

            update_neighbor_utility(cur_position, ret, possible); // Call this method to update neighboring utilities.

            for(String iter: possible)
            {
               if(iter.equals("lef") || iter.equals("rgh"))
               {
                  int x_val = Integer.parseInt(cur_position.substring(0,1));
                  int temp = iter.equals("lef")?-1:+1;
                  x_val += temp;
                  String nextTraverse =  x_val + cur_position.substring(1,cur_position.length());
                  if(!Features_states.containsKey(nextTraverse))
                  {
                     Features_states.put(nextTraverse, feature_list);
                     queue.add(nextTraverse);
                  }
               }

               else if(iter.equals("ups") || iter.equals("dwn"))
               {
                  int y_val = Integer.parseInt(cur_position.substring(2,3));
                  int temp = iter.equals("dwn")?-1:+1;
                  y_val += temp;
                  String nextTraverse =  cur_position.substring(0,2)+y_val +  cur_position.substring(3,cur_position.length());
                  if(!Features_states.containsKey(nextTraverse))
                  {
                     Features_states.put(nextTraverse, feature_list);
                     queue.add(nextTraverse);
                  }
               }

               else if(iter.equals("fwd") || iter.equals("bwd"))
               {
                  int z_val = Integer.parseInt(cur_position.substring(4,5));
                  int temp = iter.equals("fwd")?-1:+1;
                  z_val += temp;
                  String nextTraverse =  cur_position.substring(0,4)+z_val + cur_position.substring(5,cur_position.length());
                  if(!Features_states.containsKey(nextTraverse))
                  {
                     Features_states.put(nextTraverse, feature_list);
                     queue.add(nextTraverse);
                  }
               }

            }

         }

         //Make sure to update the neighboring utility values.
         public static void update_neighbor_utility(String cur_position, String utils, Set<String> validNeighbor)
         {

                  int x_val = cur_position.charAt(0)-'0';
            int y_val = cur_position.charAt(2)-'0';
            int z_val = cur_position.charAt(4)-'0';


            String[] res = utils.split(" ");

            Map<String, Double> util_vals = new HashMap<>();

            for(String j:res)
            {
               //System.out.println("j="+j);
               util_vals.put(j.split("=")[0], Double.parseDouble(j.split("=")[1]));
            }

            for(Map.Entry<String, Double> entry: util_vals.entrySet())
            {
                  String key = entry.getKey();
                  Double val = entry.getValue();

                  //System.out.println("Key="+key+"vals="+val);

            }


            //Traverse the possible neighbors and update utility based on. utility gains recorded.
            for(String neighbor: validNeighbor)
            {
               //System.out.println("neighbor===="+neighbor);

               //System.out.println("Currrr utillll value==="+util_vals.get(neighbor) * NodeGimbalVisited.get(cur_position));

               //System.out.println("Currr22222 vallllll--"+0.1 * NodeGimbalVisited.get(cur_position));
               if(neighbor.equals("lef") || neighbor.equals("rgh"))
               {

                  int new_xval = (neighbor.equals("lef")?-1:+1) + x_val;
                  String new_pos = Integer.toString(new_xval) +"_" + Integer.toString(y_val) +"_" + Integer.toString(z_val) +cur_position.substring(5,9);

                  //System.out.println("New_pos="+new_pos);

                  if(!visited[new_xval][y_val][z_val])
                  {

                     if(util_vals.get(neighbor)>0.0)
                        NodeGimbalVisited.put(new_pos, util_vals.get(neighbor) * NodeGimbalVisited.get(cur_position));
                     else if((!NodeGimbalVisited.containsKey(new_pos)))
                        NodeGimbalVisited.put(new_pos, 0.1 * NodeGimbalVisited.get(cur_position));
                  }

               }

               else if(neighbor.equals("ups") || neighbor.equals("dwn"))
               {

                  int new_yval = (neighbor.equals("dwn")?-1:+1) + y_val;
                  //System.out.println("new yval="+new_yval);
                  String new_pos = Integer.toString(x_val) +"_" + Integer.toString(new_yval) +"_" + Integer.toString(z_val) +cur_position.substring(5,9);

                  //System.out.println("New_pos="+new_pos);

                  if(!visited[x_val][new_yval][z_val])
                  {

                     if(util_vals.get(neighbor) > 0.0)
                        NodeGimbalVisited.put(new_pos, util_vals.get(neighbor) * NodeGimbalVisited.get(cur_position));
                     else if((!NodeGimbalVisited.containsKey(new_pos)))
                        NodeGimbalVisited.put(new_pos, 0.1 * NodeGimbalVisited.get(cur_position));
                  }

               }

               else if(neighbor.equals("fwd") || neighbor.equals("bck"))
               {

                  int new_zval = (neighbor.equals("fwd")?-1:+1) + z_val;
                  String new_pos = Integer.toString(x_val) +"_" + Integer.toString(y_val) +"_" + Integer.toString(new_zval) +cur_position.substring(5,9);

                  //System.out.println("New_pos="+new_pos);

                  if(!visited[x_val][y_val][new_zval])
                  {

                     if(util_vals.get(neighbor) > 0.0)
                        NodeGimbalVisited.put(new_pos, util_vals.get(neighbor) * NodeGimbalVisited.get(cur_position));
                     else if( (!NodeGimbalVisited.containsKey(new_pos)))
                        NodeGimbalVisited.put(new_pos, 0.1 * NodeGimbalVisited.get(cur_position));
                  }

               }

               else
               {
                  System.out.println("Invalid Case");
               }

            }
            //NodeGimbalVisited;
         }

         // Make sure to properly update X, Y, Z and Gimbal position. Average won't give accurate result.
         public static void convert_feature_average(Instances inputs)
         {
            //Get the count of number of instances.
            int count = inputs.numInstances();

            // Get the average feature value from Instances retrieved from KNN
            for(Instance record: inputs)
            {
               for(int i = 0; i<55; i++){
                    double val =  record.value(i);

                    if(feature_map.containsKey(i)) {
                            feature_map.put(i, feature_map.get(i)+val);
                        }
                     else
                        feature_map.put(i, val);

                    }
            }

            for(int i=0;i<55;i++)
               feature_map.replace(i,feature_map.get(i)/count); //Taking the average of the values.

            //Printing the contents of the map.
            /*System.out.println("The contents of averaging the feature values.");
            for(int i=0;i<55;i++)
               System.out.println(feature_map.get(i));
            */
         }


   public static void convert_to_map_average(Instances inputVal)
   {


      //Get the count of number of instances.
      int count = inputVal.numInstances();

      //The value from 32 to 37 holds the required utility values.
      for(Instance data:inputVal)
      {
                double curUtil = data.value(54);
                String[] keys = {"lef", "rgh", "ups", "dwn", "fwd", "bck", "g00", "g15", "g30"};

                for(int i = 0; i<9; i++){
                    double val =  data.value(55+i);

                    if(!map.containsKey(keys[i])) {
                        if(curUtil == 0 && val != 0){
                            map.put(keys[i], 1+val);
                        } else if(val > 0) {
                            map.put(keys[i], val/curUtil);
                        }
                        else
                            map.put(keys[i], val);
                    } else {
                            if(map.get(keys[i]) < (val/curUtil)) {
                                if(curUtil == 0 && val != 0){
                                    map.put(keys[i], (1+val));
                                } else {
                                    map.put(keys[i], (val/curUtil));
                                }
                        }
                    }
                }
      }
   }

   /*
      This function uses weka's K-Nearest neighbors to find the K-Nearest neighbor values and return the average of utilities of those values.
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
      //reqd.add(19); // Latitude =
      //reqd.add(20); // Longitutude =
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
      //S/ystem.out.println("Instance vals = "+curr.toStringNoWeight());


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

      // Instantiate 'knn' object.
      NearestNeighbourSearch knn = new LinearNNSearch(data);

      // KNN is run based on current psotion's feature values and offline dataset.
      Instances ins = knn.kNearestNeighbours(curr,9); // Call weka's kNearestNeighbours method.

      //System.out.println("3 NN === " +ins.toString());

      //Return the result stored in ins after running weka's kNN algorithm.
      return ins;

   }


   public static Map<String, Double> convert_astar_map_average(String cur_position, Instances inputVal, Map<String, Double> inMap)
   {

      //Get the count of number of instances.
      int count = inputVal.numInstances();

      Map<String, Double> outputMap = new HashMap<>();

      //The value from 32 to 37 holds the required utility values.
      for(Instance data:inputVal)
      {
                double curUtil = data.value(54);
                String[] keys = {"lef", "rgh", "ups", "dwn", "fwd", "bck", "g00", "g15", "g30"};

                for(int i = 0; i<9; i++){
                    double val =  data.value(55+i);

                    if(!inMap.containsKey(keys[i])) {
                        if(curUtil == 0 && val != 0){
                            inMap.put(keys[i], 1+val);
                        } else if(val > 0) {
                            inMap.put(keys[i], val/curUtil);
                        }
                        else
                            inMap.put(keys[i], val);
                    } else {
                            if(inMap.get(keys[i]) < (val/curUtil)) {
                                if(curUtil == 0 && val != 0){
                                    inMap.put(keys[i], (1+val));
                                } else {
                                    inMap.put(keys[i], (val/curUtil));
                                }
                        }
                    }
                }
             }

      int x_val = cur_position.charAt(0)-'0';
      int y_val = cur_position.charAt(2)-'0';
      int z_val = cur_position.charAt(4)-'0';

      for(Map.Entry<String, Double> entry: inMap.entrySet())
      {
         String neighbor = entry.getKey();
         Double val = entry.getValue();

         //System.out.println("Key="+key+"vals="+val);

      if(neighbor.equals("lef") || neighbor.equals("rgh"))
      {

         int new_xval = (neighbor.equals("lef")?-1:+1) + x_val;
         String new_pos = Integer.toString(new_xval) +"_" + Integer.toString(y_val) +"_" + Integer.toString(z_val) +cur_position.substring(5,9);

         //System.out.println("New_pos="+new_pos);

         outputMap.put(new_pos, val);

      }

      else if(neighbor.equals("ups") || neighbor.equals("dwn"))
      {

         int new_yval = (neighbor.equals("dwn")?-1:+1) + y_val;
         //System.out.println("new yval="+new_yval);
         String new_pos = Integer.toString(x_val) +"_" + Integer.toString(new_yval) +"_" + Integer.toString(z_val) +cur_position.substring(5,9);

         //System.out.println("New_pos="+new_pos);
          outputMap.put(new_pos, val);
      }

      else if(neighbor.equals("fwd") || neighbor.equals("bck"))
      {

         int new_zval = (neighbor.equals("fwd")?-1:+1) + z_val;
         String new_pos = Integer.toString(x_val) +"_" + Integer.toString(y_val) +"_" + Integer.toString(new_zval) +cur_position.substring(5,9);

         //System.out.println("New_pos="+new_pos);
         outputMap.put(new_pos, val);

      }

      else if(neighbor.equals("g00") || neighbor.equals("g15") || neighbor.equals("g30"))
      {
         String gim_next = neighbor.equals("g00")?"g00" : neighbor.equals("g15")?"g15" : "g30";

         String new_pos = Integer.toString(x_val) +"_" + Integer.toString(y_val) +"_" + Integer.toString(z_val) + "_" + gim_next;

         outputMap.put(new_pos, val);
      }


      }
      return outputMap;
   }

   /*
      Implementation of A-star Algorithm.
      Input the start state, goal state

      Possible Output:  -> List containing path from start to end.
                        -> Next Move to be taken.


   */

      public static String A_star_search(String start, String goal) throws Exception
      {
         Node _0_0_0_g00 = new Node("0_0_0_g00",NodeGimbalVisited.get("0_0_0_g00"));
         Node _0_0_0_g15 = new Node("0_0_0_g15",NodeGimbalVisited.get("0_0_0_g15"));
         Node _0_0_0_g30 = new Node("0_0_0_g30",NodeGimbalVisited.get("0_0_0_g30"));
         Node _0_0_1_g00 = new Node("0_0_1_g00",NodeGimbalVisited.get("0_0_1_g00"));
         Node _0_0_1_g15 = new Node("0_0_1_g15",NodeGimbalVisited.get("0_0_1_g15"));
         Node _0_0_1_g30 = new Node("0_0_1_g30",NodeGimbalVisited.get("0_0_1_g30"));
         Node _0_1_0_g00 = new Node("0_1_0_g00",NodeGimbalVisited.get("0_1_0_g00"));
         Node _0_1_0_g15 = new Node("0_1_0_g15",NodeGimbalVisited.get("0_1_0_g15"));
         Node _0_1_0_g30 = new Node("0_1_0_g30",NodeGimbalVisited.get("0_1_0_g30"));
         Node _0_1_1_g00 = new Node("0_1_1_g00",NodeGimbalVisited.get("0_1_1_g00"));
         Node _0_1_1_g15 = new Node("0_1_1_g15",NodeGimbalVisited.get("0_1_1_g15"));
         Node _0_1_1_g30 = new Node("0_1_1_g30",NodeGimbalVisited.get("0_1_1_g30"));
         Node _0_2_0_g00 = new Node("0_2_0_g00",NodeGimbalVisited.get("0_2_0_g00"));
         Node _0_2_0_g15 = new Node("0_2_0_g15",NodeGimbalVisited.get("0_2_0_g15"));
         Node _0_2_0_g30 = new Node("0_2_0_g30",NodeGimbalVisited.get("0_2_0_g30"));
         Node _0_2_1_g00 = new Node("0_2_1_g00",NodeGimbalVisited.get("0_2_1_g00"));
         Node _0_2_1_g15 = new Node("0_2_1_g15",NodeGimbalVisited.get("0_2_1_g15"));
         Node _0_2_1_g30 = new Node("0_2_1_g30",NodeGimbalVisited.get("0_2_1_g30"));
         Node _1_0_0_g00 = new Node("1_0_0_g00",NodeGimbalVisited.get("1_0_0_g00"));
         Node _1_0_0_g15 = new Node("1_0_0_g15",NodeGimbalVisited.get("1_0_0_g15"));
         Node _1_0_0_g30 = new Node("1_0_0_g30",NodeGimbalVisited.get("1_0_0_g30"));
         Node _1_0_1_g00 = new Node("1_0_1_g00",NodeGimbalVisited.get("1_0_1_g00"));
         Node _1_0_1_g15 = new Node("1_0_1_g15",NodeGimbalVisited.get("1_0_1_g15"));
         Node _1_0_1_g30 = new Node("1_0_1_g30",NodeGimbalVisited.get("1_0_1_g30"));
         Node _1_1_0_g00 = new Node("1_1_0_g00",NodeGimbalVisited.get("1_1_0_g00"));
         Node _1_1_0_g15 = new Node("1_1_0_g15",NodeGimbalVisited.get("1_1_0_g15"));
         Node _1_1_0_g30 = new Node("1_1_0_g30",NodeGimbalVisited.get("1_1_0_g30"));
         Node _1_2_0_g00 = new Node("1_2_0_g00",NodeGimbalVisited.get("1_2_0_g00"));
         Node _1_2_0_g15 = new Node("1_2_0_g15",NodeGimbalVisited.get("1_2_0_g15"));
         Node _1_2_0_g30 = new Node("1_2_0_g30",NodeGimbalVisited.get("1_2_0_g30"));
         Node _1_2_1_g00 = new Node("1_2_1_g00",NodeGimbalVisited.get("1_2_0_g00"));
         Node _1_2_1_g15 = new Node("1_2_1_g15",NodeGimbalVisited.get("1_2_0_g15"));
         Node _1_2_1_g30 = new Node("1_2_1_g30",NodeGimbalVisited.get("1_2_0_g30"));
         Node _1_1_1_g00 = new Node("1_1_1_g00",NodeGimbalVisited.get("1_1_0_g00"));
         Node _1_1_1_g15 = new Node("1_1_1_g15",NodeGimbalVisited.get("1_1_0_g15"));
         Node _1_1_1_g30 = new Node("1_1_1_g30",NodeGimbalVisited.get("1_1_0_g30"));

         //Construct a HashMap to store <String, Node> pairs which could be retrieved easily.
         Map<String, Node> nodeMap = new HashMap<>();

         nodeMap.put("_0_0_0_g00", _0_0_0_g00); //#1
         nodeMap.put("_0_0_0_g15", _0_0_0_g15); //#2
         nodeMap.put("_0_0_0_g30", _0_0_0_g30); //#3
         nodeMap.put("_0_0_1_g00", _0_0_1_g00); //#4
         nodeMap.put("_0_0_1_g15", _0_0_1_g15); //#5
         nodeMap.put("_0_0_1_g30", _0_0_1_g30); //#6
         nodeMap.put("_0_1_0_g00", _0_1_0_g00); //#7
         nodeMap.put("_0_1_0_g15", _0_1_0_g15); //#8
         nodeMap.put("_0_1_0_g30", _0_1_0_g30); //#9
         nodeMap.put("_0_1_1_g00", _0_1_1_g00); //#10
         nodeMap.put("_0_1_1_g15", _0_1_1_g15); //#11
         nodeMap.put("_0_1_1_g30", _0_1_1_g30); //#12
         nodeMap.put("_0_2_0_g00", _0_2_0_g00); //#13
         nodeMap.put("_0_2_0_g15", _0_2_0_g15); //#14
         nodeMap.put("_0_2_0_g30", _0_2_0_g30); //#15
         nodeMap.put("_0_2_1_g00", _0_2_1_g00); //#16
         nodeMap.put("_0_2_1_g15", _0_2_1_g15); //#17
         nodeMap.put("_0_2_1_g30", _0_2_1_g30); //#18
         nodeMap.put("_1_0_0_g00", _1_0_0_g00); //#19
         nodeMap.put("_1_0_0_g15", _1_0_0_g15); //#20
         nodeMap.put("_1_0_0_g30", _1_0_0_g30); //#21]
         nodeMap.put("_1_0_1_g00", _1_0_1_g00); //#22
         nodeMap.put("_1_0_1_g15", _1_0_1_g15); //#23
         nodeMap.put("_1_0_1_g30", _1_0_1_g30); //#24
         nodeMap.put("_1_1_0_g00", _1_1_0_g00); //#25
         nodeMap.put("_1_1_0_g15", _1_1_0_g15); //#26
         nodeMap.put("_1_1_0_g30", _1_1_0_g30); //#27
         nodeMap.put("_1_2_0_g00", _1_2_0_g00); //#28
         nodeMap.put("_1_2_0_g15", _1_2_0_g15); //#29
         nodeMap.put("_1_2_0_g30", _1_2_0_g30); //#30
         nodeMap.put("_1_2_1_g00", _1_2_1_g00); //#31
         nodeMap.put("_1_2_1_g15", _1_2_1_g15); //#32
         nodeMap.put("_1_2_1_g30", _1_2_1_g30); //#33
         nodeMap.put("_1_1_1_g00", _1_1_1_g00); //#34
         nodeMap.put("_1_1_1_g15", _1_1_1_g15); //#35
         nodeMap.put("_1_1_1_g30", _1_1_1_g30); //#36


         //System.out.println("Map valssssss="+(nodeMap.get("_0_0_0_g00").value));

         Map<String, Double> weight_values = getWeights();
         /*for(Map.Entry<String, Double> entry: weight_values.entrySet())
         {
            String key = entry.getKey();
            Double val = entry.getValue();

            System.out.println("Key= "+key+" vals="+val);

         }*/

         //Init the required edges
         _0_0_0_g00.adjacencies = new Edge[]{ //#1
                        new Edge(_0_0_0_g15, weight_values.get("g15")),
                        new Edge(_0_0_0_g30, weight_values.get("g30")),
                        new Edge(_0_0_1_g00, weight_values.get("bck")),
                        new Edge(_0_1_0_g00, weight_values.get("ups")),
                        new Edge(_1_0_0_g00, weight_values.get("rgh"))
                };

         _0_0_0_g15.adjacencies = new Edge[]{ //#2
                        new Edge(_0_0_0_g00, weight_values.get("g00")),
                        new Edge(_0_0_0_g30, weight_values.get("g30")),
                        new Edge(_0_0_1_g15, weight_values.get("bck")),
                        new Edge(_0_1_0_g15, weight_values.get("ups")),
                        new Edge(_1_0_0_g15, weight_values.get("rgh"))
                };

         _0_0_0_g30.adjacencies = new Edge[]{ //#3
                        new Edge(_0_0_0_g15, weight_values.get("g15")),
                        new Edge(_0_0_0_g00, weight_values.get("g00")),
                        new Edge(_0_0_1_g30, weight_values.get("bck")),
                        new Edge(_0_1_0_g30, weight_values.get("ups")),
                        new Edge(_1_0_0_g30, weight_values.get("rgh"))
                };

         _0_0_1_g00.adjacencies = new Edge[]{ //#4
                        new Edge(_0_0_1_g15, weight_values.get("g15")),
                        new Edge(_0_0_1_g30, weight_values.get("g30")),
                        new Edge(_0_0_0_g00, weight_values.get("fwd")),
                        new Edge(_0_1_1_g00, weight_values.get("ups")),
                        new Edge(_1_0_1_g00, weight_values.get("rgh"))
                };

         _0_0_1_g15.adjacencies = new Edge[]{ //#5
                        new Edge(_0_0_1_g00, weight_values.get("g15")),
                        new Edge(_0_0_1_g30, weight_values.get("g30")),
                        new Edge(_0_0_0_g15, weight_values.get("fwd")),
                        new Edge(_0_1_1_g15, weight_values.get("ups")),
                        new Edge(_1_0_1_g15, weight_values.get("rgh"))
                };

         _0_0_1_g30.adjacencies = new Edge[]{ //#6
                        new Edge(_0_0_1_g15, weight_values.get("g15")),
                        new Edge(_0_0_1_g00, weight_values.get("g00")),
                        new Edge(_0_0_0_g30, weight_values.get("fwd")),
                        new Edge(_0_1_1_g30, weight_values.get("ups")),
                        new Edge(_1_0_1_g30, weight_values.get("rgh"))
                };


         _0_1_0_g00.adjacencies = new Edge[]{  //#7
                        new Edge(_0_1_0_g15, weight_values.get("g15")),
                        new Edge(_0_1_0_g30, weight_values.get("g30")),
                        new Edge(_0_1_1_g00, weight_values.get("bck")),
                        new Edge(_0_0_0_g00, weight_values.get("dwn")),
                        new Edge(_1_1_0_g00, weight_values.get("rgh"))
                };

         _0_1_0_g15.adjacencies = new Edge[]{  //#8
                        new Edge(_0_1_0_g00, weight_values.get("g00")),
                        new Edge(_0_1_0_g30, weight_values.get("g30")),
                        new Edge(_0_1_1_g15, weight_values.get("bck")),
                        new Edge(_0_0_0_g15, weight_values.get("dwn")),
                        new Edge(_1_1_0_g15, weight_values.get("rgh"))
                };

         _0_1_0_g30.adjacencies = new Edge[]{  //#9
                        new Edge(_0_1_0_g15, weight_values.get("g15")),
                        new Edge(_0_1_0_g00, weight_values.get("g00")),
                        new Edge(_0_1_1_g30, weight_values.get("bck")),
                        new Edge(_0_0_0_g30, weight_values.get("dwn")),
                        new Edge(_1_1_0_g30, weight_values.get("rgh"))
                };

         _0_1_1_g00.adjacencies = new Edge[]{ //#10
                        new Edge(_0_1_1_g15, weight_values.get("g15")),
                        new Edge(_0_1_1_g30, weight_values.get("g30")),
                        new Edge(_0_0_1_g00, weight_values.get("dwn")),
                        new Edge(_0_1_0_g00, weight_values.get("fwd")),
                        new Edge(_1_1_1_g00, weight_values.get("rgh"))
                };

         _0_1_1_g15.adjacencies = new Edge[]{ //#11
                        new Edge(_0_1_1_g00, weight_values.get("g00")),
                        new Edge(_0_1_1_g30, weight_values.get("g30")),
                        new Edge(_0_0_1_g15, weight_values.get("dwn")),
                        new Edge(_0_1_0_g15, weight_values.get("fwd")),
                        new Edge(_1_1_1_g15, weight_values.get("rgh"))
                };

         _0_1_1_g30.adjacencies = new Edge[]{ //#12
                        new Edge(_0_1_1_g15, weight_values.get("g15")),
                        new Edge(_0_1_1_g00, weight_values.get("g00")),
                        new Edge(_0_0_1_g30, weight_values.get("dwn")),
                        new Edge(_0_1_0_g30, weight_values.get("fwd")),
                        new Edge(_1_1_1_g30, weight_values.get("rgh"))
                };

         _0_2_0_g00.adjacencies = new Edge[]{ //#13
                        new Edge(_0_2_0_g15, weight_values.get("g15")),
                        new Edge(_0_2_0_g30, weight_values.get("g30")),
                        new Edge(_0_2_1_g00, weight_values.get("bck")),
                        new Edge(_0_1_0_g00, weight_values.get("dwn")),
                        new Edge(_1_2_0_g00, weight_values.get("rgh"))
                };

         _0_2_0_g15.adjacencies = new Edge[]{ //#14
                        new Edge(_0_2_0_g00, weight_values.get("g00")),
                        new Edge(_0_2_0_g30, weight_values.get("g30")),
                        new Edge(_0_2_1_g15, weight_values.get("bck")),
                        new Edge(_0_1_0_g15, weight_values.get("dwn")),
                        new Edge(_1_2_0_g15, weight_values.get("rgh"))
                };

         _0_2_0_g30.adjacencies = new Edge[]{ //#15
                        new Edge(_0_2_0_g15, weight_values.get("g15")),
                        new Edge(_0_2_0_g00, weight_values.get("g00")),
                        new Edge(_0_2_1_g30, weight_values.get("bck")),
                        new Edge(_0_1_0_g30, weight_values.get("dwn")),
                        new Edge(_1_2_0_g30, weight_values.get("rgh"))
                };

         _0_2_1_g00.adjacencies = new Edge[]{ //#16
                        new Edge(_0_2_1_g15, weight_values.get("g15")),
                        new Edge(_0_2_1_g30, weight_values.get("g30")),
                        new Edge(_0_2_0_g00, weight_values.get("fwd")),
                        new Edge(_0_1_1_g00, weight_values.get("dwn")),
                        new Edge(_1_2_1_g00, weight_values.get("rgh"))
                };

         _0_2_1_g15.adjacencies = new Edge[]{ //#17
                        new Edge(_0_2_1_g00, weight_values.get("g00")),
                        new Edge(_0_2_1_g30, weight_values.get("g30")),
                        new Edge(_0_2_0_g15, weight_values.get("fwd")),
                        new Edge(_0_1_1_g15, weight_values.get("dwn")),
                        new Edge(_1_2_1_g15, weight_values.get("rgh"))
                };

        _0_2_1_g30.adjacencies = new Edge[]{ //#18
                        new Edge(_0_2_1_g15, weight_values.get("g15")),
                        new Edge(_0_2_1_g00, weight_values.get("g00")),
                        new Edge(_0_2_0_g30, weight_values.get("fwd")),
                        new Edge(_0_1_1_g30, weight_values.get("dwn")),
                        new Edge(_1_2_1_g30, weight_values.get("rgh"))
                };

         _1_2_1_g00.adjacencies = new Edge[]{ //#19
                        new Edge(_1_2_1_g15, weight_values.get("g15")),
                        new Edge(_1_2_1_g30, weight_values.get("g30")),
                        new Edge(_1_2_0_g00, weight_values.get("fwd")),
                        new Edge(_0_2_1_g00, weight_values.get("lef")),
                        new Edge(_1_1_1_g00, weight_values.get("dwn"))
                };

         _1_2_1_g15.adjacencies = new Edge[]{ //#20
                        new Edge(_1_2_1_g00, weight_values.get("g00")),
                        new Edge(_1_2_1_g30, weight_values.get("g30")),
                        new Edge(_1_2_0_g15, weight_values.get("fwd")),
                        new Edge(_0_2_1_g15, weight_values.get("lef")),
                        new Edge(_1_1_1_g15, weight_values.get("dwn"))
                };

        _1_2_1_g30.adjacencies = new Edge[]{ //#21
                        new Edge(_1_2_1_g15, weight_values.get("g15")),
                        new Edge(_1_2_1_g00, weight_values.get("g00")),
                        new Edge(_1_2_0_g30, weight_values.get("fwd")),
                        new Edge(_0_2_1_g30, weight_values.get("lef")),
                        new Edge(_1_1_1_g30, weight_values.get("dwn"))
                };

        _1_0_1_g00.adjacencies = new Edge[]{ //#22
                        new Edge(_1_0_1_g15, weight_values.get("g15")),
                        new Edge(_1_0_1_g30, weight_values.get("g30")),
                        new Edge(_1_0_0_g00, weight_values.get("fwd")),
                        new Edge(_0_0_1_g00, weight_values.get("lef")),
                        new Edge(_1_1_1_g00, weight_values.get("ups"))
                };

         //Init the required edges
         _1_0_1_g15.adjacencies = new Edge[]{ //#23
                        new Edge(_1_0_1_g00, weight_values.get("g00")),
                        new Edge(_1_0_1_g30, weight_values.get("g30")),
                        new Edge(_1_0_0_g15, weight_values.get("fwd")),
                        new Edge(_0_0_1_g15, weight_values.get("lef")),
                        new Edge(_1_1_1_g15, weight_values.get("ups"))
                };

         _1_0_1_g30.adjacencies = new Edge[]{ //#24
                        new Edge(_1_0_1_g15, weight_values.get("g15")),
                        new Edge(_1_0_1_g00, weight_values.get("g00")),
                        new Edge(_1_0_0_g30, weight_values.get("fwd")),
                        new Edge(_0_0_1_g30, weight_values.get("lef")),
                        new Edge(_1_1_1_g30, weight_values.get("ups"))
                };

         _1_1_1_g00.adjacencies = new Edge[]{ //#25
                        new Edge(_1_1_1_g15, weight_values.get("g15")),
                        new Edge(_1_1_1_g30, weight_values.get("g15")),
                        new Edge(_1_1_0_g00, weight_values.get("g15")),
                        new Edge(_0_1_1_g00, weight_values.get("g15")),
                        new Edge(_1_0_1_g00, weight_values.get("g15")),
                        new Edge(_1_2_1_g00, weight_values.get("g15"))
                };

         _1_1_1_g15.adjacencies = new Edge[]{ //#26
                        new Edge(_1_1_1_g00, weight_values.get("g00")),
                        new Edge(_1_1_1_g30, weight_values.get("g30")),
                        new Edge(_1_1_0_g15, weight_values.get("fwd")),
                        new Edge(_0_1_1_g15, weight_values.get("lef")),
                        new Edge(_1_0_1_g15, weight_values.get("dwn")),
                        new Edge(_1_2_1_g15, weight_values.get("ups"))
                };

         _1_1_1_g30.adjacencies = new Edge[]{ //#27
                        new Edge(_1_1_1_g15, weight_values.get("g15")),
                        new Edge(_1_1_1_g00, weight_values.get("g00")),
                        new Edge(_1_1_0_g30, weight_values.get("fwd")),
                        new Edge(_0_1_1_g30, weight_values.get("lef")),
                        new Edge(_1_0_1_g30, weight_values.get("dwn")),
                        new Edge(_1_2_1_g30, weight_values.get("ups"))
                };

         _1_2_0_g00.adjacencies = new Edge[]{ //#28
                        new Edge(_1_2_0_g15, weight_values.get("g15")),
                        new Edge(_1_2_0_g30, weight_values.get("g30")),
                        new Edge(_1_1_0_g00, weight_values.get("dwn")),
                        new Edge(_1_2_1_g00, weight_values.get("bck")),
                        new Edge(_0_2_0_g00, weight_values.get("lef"))
                };

         _1_2_0_g15.adjacencies = new Edge[]{ //#29
                        new Edge(_1_2_0_g00, weight_values.get("g15")),
                        new Edge(_1_2_0_g30, weight_values.get("g30")),
                        new Edge(_1_1_0_g15, weight_values.get("dwn")),
                        new Edge(_1_2_1_g15, weight_values.get("fwd")),
                        new Edge(_0_2_0_g15, weight_values.get("lef"))
                };

         _1_2_0_g30.adjacencies = new Edge[]{ //#30
                        new Edge(_1_2_0_g15, weight_values.get("g15")),
                        new Edge(_1_2_0_g00, weight_values.get("g00")),
                        new Edge(_1_1_0_g30, weight_values.get("dwn")),
                        new Edge(_1_2_1_g30, weight_values.get("bck")),
                        new Edge(_0_2_0_g30, weight_values.get("lef"))
                };;

         _1_0_0_g00.adjacencies = new Edge[]{ //#31
                        new Edge(_1_0_0_g15, weight_values.get("g15")),
                        new Edge(_1_0_0_g30, weight_values.get("g30")),
                        new Edge(_1_0_1_g00, weight_values.get("bck")),
                        new Edge(_1_1_0_g00, weight_values.get("ups")),
                        new Edge(_0_0_0_g00, weight_values.get("lef"))
                };

         _1_0_0_g15.adjacencies = new Edge[]{ //#32
                        new Edge(_1_0_0_g00, weight_values.get("g00")),
                        new Edge(_1_0_0_g30, weight_values.get("g30")),
                        new Edge(_1_0_1_g15, weight_values.get("bck")),
                        new Edge(_1_1_0_g15, weight_values.get("ups")),
                        new Edge(_0_0_0_g15, weight_values.get("lef"))
                };

         _1_0_0_g30.adjacencies = new Edge[]{ //#33
                        new Edge(_1_0_0_g15, weight_values.get("g15")),
                        new Edge(_1_0_0_g00, weight_values.get("g00")),
                        new Edge(_1_0_1_g30, weight_values.get("bck")),
                        new Edge(_1_1_0_g30, weight_values.get("ups")),
                        new Edge(_0_0_0_g30, weight_values.get("lef"))
                };

         _1_1_0_g00.adjacencies = new Edge[]{ //#34
                        new Edge(_1_1_0_g15, weight_values.get("g15")),
                        new Edge(_1_1_0_g30, weight_values.get("g30")),
                        new Edge(_1_0_0_g00, weight_values.get("dwn")),
                        new Edge(_1_1_1_g00, weight_values.get("bck")),
                        new Edge(_0_1_0_g00, weight_values.get("lef")),
                        new Edge(_1_2_0_g00, weight_values.get("ups"))
                };

         _1_1_0_g15.adjacencies = new Edge[]{ //#35
                        new Edge(_1_1_0_g00, weight_values.get("g00")),
                        new Edge(_1_1_0_g30, weight_values.get("g30")),
                        new Edge(_1_0_0_g15, weight_values.get("dwn")),
                        new Edge(_1_1_1_g15, weight_values.get("bck")),
                        new Edge(_0_1_0_g15, weight_values.get("lef")),
                        new Edge(_1_2_0_g15, weight_values.get("ups"))
                };

         _1_1_0_g30.adjacencies = new Edge[]{ //#36
                        new Edge(_1_1_0_g15, weight_values.get("g15")),
                        new Edge(_1_1_0_g00, weight_values.get("g00")),
                        new Edge(_1_0_0_g30, weight_values.get("dwn")),
                        new Edge(_1_1_1_g30, weight_values.get("bck")),
                        new Edge(_0_1_0_g30, weight_values.get("lef")),
                        new Edge(_1_2_0_g30, weight_values.get("ups"))
                };

         String start_update = "_" + start; // Update start to match node name.
         String goal_update = "_" + goal; //Update goal to match the node name.

         //System.out.println("start_update=="+start_update);
         //System.out.println("goal update=="+goal_update);
         //System.out.println(goal_update);
         AstarSearch(nodeMap.get(start_update), nodeMap.get(goal_update));

         //AstarSearch(start_update, goal_update);
         String print = "_0_0_0_g00";
         List<Node> path = printPath(nodeMap.get(goal_update));

         //System.out.println("source= 1_1_1_g00, Dest = 0_0_0_g00 and the Path: " + path);

         /*for(Node i:path)
            System.out.println("node --- "+i.value);
         */
         //System.out.println("The next direction to take is....");

         if(start.equals(goal))
            return "lnd"; //Return a land command.

         String current_pos = path.get(0).value;
         //System.out.println("cur = " + current_pos);

         String next_pos = path.get(1).value;
         //System.out.println("nexttt = " + next_pos);

         //for(Node n : path)
            // System.out.println(n.value);

         //System.out.println("Curr gimb pos="+current_pos.substring(6,9));
         //Check the movement to return.
         if(current_pos.charAt(0) != next_pos.charAt(0))
         {
            return ((current_pos.charAt(0)-'0') - ((next_pos.charAt(0)-'0')) == 1?"rgh":"lef");
         }

         else if(current_pos.charAt(2) != next_pos.charAt(2))
         {
            return ((current_pos.charAt(2)-'0') - ((next_pos.charAt(2)-'0')) == 1?"dwn":"ups");
         }

         else if(current_pos.charAt(4) != next_pos.charAt(4))
         {
            return ((current_pos.charAt(4)-'0') - ((next_pos.charAt(4)-'0')) == 1?"bck":"fwd");
         }
         else if(!current_pos.substring(6,9).equals(next_pos.substring(6,9)))
         {
            //int cur_gimb = Integer.parseInt(current_pos.substring(6,9));
            //int nex_gimb = Integer.parseInt(next_pos.substring(6,9));

            return next_pos.substring(6,9);

         }

         //Invalid case, return land.
         return "lnd";
      }

      public static void AstarSearch(Node source, Node goal){


         //Node source = new Node(src, )
         Set<Node> explored = new HashSet<Node>();

         PriorityQueue<Node> queue = new PriorityQueue<Node>(36,
               new Comparator<Node>(){
               //override compare method
               public int compare(Node i, Node j){
                  if(i.f_value > j.f_value){
                        return 1;
                    }

                  else if (i.f_value < j.f_value){
                        return -1;
                    }

                  else{
                        return 0;
                  }
               }

            }
            );

         //cost from start
         source.g_value = 0;

         queue.add(source);

         boolean found = false;

         while((!queue.isEmpty())&&(!found)){

            //the node in having the lowest f_score value
            Node current = queue.poll();

            explored.add(current);

            //goal found
            if(current.value.equals(goal.value)){
               found = true;
               }

            //check every child of current node
            for(Edge e : current.adjacencies){
               Node child = e.target;
               double cost = e.cost;
               double temp_g_value = current.g_value + 1/cost; // We should be taking teh reciprocal of weight
               double temp_f_value = temp_g_value + child.h_value;


               /*if child node has been evaluated and
               the newer f_score is higher, skip*/

               if((explored.contains(child)) && (temp_f_value >= child.f_value)){
                  continue;
                  }

               /*else if child node is not in queue or
               newer f_score is lower*/

               else if((!queue.contains(child)) ||
               (temp_f_value < child.f_value)){

               child.parent = current;
               child.g_value = temp_g_value;
               child.f_value = temp_f_value;

               if(queue.contains(child)){
                  queue.remove(child);
                  }

               queue.add(child);

               }

            }

         }

   }


   public static List<Node> printPath(Node target){
                List<Node> path = new ArrayList<Node>();

        for(Node node = target; node!=null; node = node.parent){
            path.add(node);
        }

        Collections.reverse(path);

        return path;
        }



      //print the shortest path from the destination to the source.
      public static String find_shortest_path_src_dest(String start, String goal, Map<String, Double> distance_GN)
      {

         //String path_taken = goal; //Set the start node as initial starting point.
         String start_point = start.substring(0, 5);
         String goal_reaching = goal.substring(0, 5);

         //boolean[][][][] vis_states = new boolean[2][3][2][3];

         Set<String> vis_states = new HashSet<>();
         Queue<List<String>> q = new LinkedList<>();

         List<String> curStatePath = new ArrayList<>();
         curStatePath.add(start_point);
         q.add(curStatePath);

         while(!q.isEmpty())
         {
            curStatePath = q.poll();

            start_point = curStatePath.get(curStatePath.size() - 1);

            if(start_point.equals(goal_reaching))
            {
               //print the path
               System.out.println("found the solution...");
               System.out.println(curStatePath);
               break;

            }

            int x_co = start_point.charAt(0) - '0';
            int y_co = start_point.charAt(2) - '0';
            int z_co = start_point.charAt(4) - '0';
            //int gimbal_pos = Integer.parseInt(current.substring(7,9));
            start_point+= start.substring(5,9);
           // System.out.println("vallll---"+start_point);
            Set<String> possible_state = find_one_hop_neighbor_values(start_point);


            //System.out.println("Possible states are as follows:");
            //Print all the possible states.
            for(String next:possible_state)
            {
               String neighbor = "";
               if(next.equals("lef") || next.equals("rgh"))
               {

                  int new_xval = (next.equals("lef")?-1:+1) + x_co;
                   neighbor = Integer.toString(new_xval) +"_" + Integer.toString(y_co) +"_" + Integer.toString(z_co) +start.substring(5,9);

                  //System.out.println("New_pos="+new_pos);

               }

               else if(next.equals("ups") || next.equals("dwn"))
               {


                  int new_yval = (next.equals("dwn")?-1:+1) + y_co;
                  //System.out.println("new yval="+new_yval);
                   neighbor = Integer.toString(x_co) +"_" + Integer.toString(new_yval) +"_" + Integer.toString(z_co) +start.substring(5,9);

               }

               else if(next.equals("fwd") || next.equals("bck"))
               {


                  int new_zval = (next.equals("fwd")?-1:+1) + z_co;
                   neighbor = Integer.toString(x_co) +"_" + Integer.toString(y_co) +"_" + Integer.toString(new_zval) +start.substring(5,9);

               }

               //System.out.println(i);
               if(!vis_states.contains(neighbor))
               {

                  vis_states.add(neighbor.substring(0,5));
                  List<String> nextNodePath = new ArrayList<>(curStatePath);
                  curStatePath.add(neighbor.substring(0,5));
                  q.add(curStatePath);
               }
            }

         }

         return "lef";
      }

      //This method is used to initialize the HashMap with very big values. Once visited, we will have lower values that can be choosen from.
      public static Map<String, Double> Initialize_big_value(Map<String, Double> dist)
      {

         for(int i=0;i<2;i++)
         {
            for(int j=0;j<3;j++)
            {
               for(int k=0;k<2;k++)
               {
                  String curr = i + "_" + j + "_" + k;
                  dist.put(curr+"_g00", Double.MAX_VALUE);
                  dist.put(curr+"_g15", Double.MAX_VALUE);
                  dist.put(curr+"_g30", Double.MAX_VALUE);

               }
            }
         }
         return dist;
      }

      // Method to get the input from a file and feed it to the list.
      public static List<Double> getInput(){
        ArrayList<Double> ret = new ArrayList<Double>();
        try{
            BufferedReader reader = new BufferedReader(new FileReader("input"));
            String line;
            while((line = reader.readLine()) != null) {
                String[] feats = line.substring(1,line.length()-1).split(",");
                for(String feat : feats){
                    ret.add(Double.parseDouble(feat.trim().split("=")[1]));
                    if(feat.trim().split("=")[0].equals("Utility"))
                        utility = ret.get(ret.size()-1);
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        return ret;
    }


    //Method to read weights from a file and return them.
    public static Map<String, Double> getWeights(){

        Map<String , Double> weights = new HashMap<>(); //A hashMap to store weights reqd.
        try{
            BufferedReader reader = new BufferedReader(new FileReader("weights_astar"));
            String line;
            while((line = reader.readLine()) != null) {
                String[] feats = line.substring(1,line.length()-1).split(",");
                for(String feat : feats){
                    weights.put(feat.trim().split("=")[0], Double.parseDouble(feat.trim().split("=")[1]));
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        return weights;
    }

      public static void main(String[] args) throws Exception {

         // 1. Take image at current position and extract feature values.
         //cur_pos = "0_1_0_g00";
         // 2. The offline estimated cost-values would be stored in a 2x3x2 Double array. (Think about this should u use more??? like 3x3x3)
         //Double[][][] estimated_cost = new Double[2][3][2];

         List<Double> list = new ArrayList<>();
         //Populate the dummy list with appropriate dummy values

         list = getInput();

         Double cur_img_utility = (list.get(54)) ;
         int x_position = (list.get(16)).intValue();
         int y_position = (list.get(17)).intValue();
         int z_position = (list.get(18)).intValue();

         //System.out.println("main gimbal from list="+list.get(29));

         String gimbal_position = ((list.get(29) == 0.0)?"g00":(list.get(29) == -0.5)?"g15":"g30");

         String cur_position =  x_position + "_" + y_position + "_" + z_position + "_" + gimbal_position;;

         //System.out.println("main cur pos is equal to ="+cur_position);


          //Store the states to be traversed nodes in a Queue. Define a queue and perform this operation.
         queue.add(cur_position);

         // Repeatedly call construct map until queue is empty.
         while(!queue.isEmpty())
         {
            String cur_pos = queue.remove();
            //System.out.println("Value removed = "+cur_pos);
            construct_offline_map(list, cur_pos);
         }

         // Identify the possible goal nodes.
         //System.out.println("Identifying the goal nodes and adding them to a Map:");
         for(Map.Entry<String, Double> entry: NodeGimbalVisited.entrySet())
         {
            String key = entry.getKey();
            Double val = entry.getValue();

            //if(val > reqd_threshold)
            goal_nodes.put(key, val);

            //System.out.println("Key= "+key+" vals="+val);

         }

         //System.out.println("The goal_node values are as follows:");

         String goal_toReach = get_goal_nodes();
         String direction = A_star_search(cur_position, goal_toReach);

         System.out.println(direction+"=2 "+"Utility="+cur_img_utility);
      }
   }

   class Node
   {


      public final String value;
      public double g_value;
      public double h_value;
      public double f_value = 0;
      //public String position;
      public Edge[] adjacencies;
      public Node parent;

      public Node(String pos, double hVal)
      {
         value = pos;
         h_value = hVal;
      }

      public String toString(){
                return value;
        }

}

class Edge{
        public final double cost;
        public final Node target;

        public Edge(Node targetNode, double costVal){
                target = targetNode;
                cost = costVal;
        }
}
