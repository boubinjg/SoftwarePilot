import java.lang.*;
import java.io.*;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Arrays;
class Parser {
    public String JSON = "";
    public String Image = "";
    public String Yaml = "";
    public int top=-1, right=-1, bottom=-1, left=-1;
    public int lbpTop=-1, lbpRight=-1, lbpBottom=-1, lbpLeft=-1;
    public int haarTop=-1, haarRight=-1, haarBottom=-1, haarLeft=-1;
    public ArrayList<String> script, feature;
    public ArrayList<Hashtable<String, ArrayList<String>>> scriptToFeatures;
    public ArrayList<ArrayList<String>> returnFeatures;
    public Hashtable<String, Boolean> zeroFeatures = new Hashtable<String, Boolean>();
    public String getFeature(String line, int num) throws Exception{
        feature.set(num, line.split("=")[0]);
        String[] args = line.split("=")[1].split(",");
        script.set(num, args[0]);
        if(scriptToFeatures.get(num).containsKey(script.get(num))) {
            for(String s : scriptToFeatures.get(num).get(script.get(num))) {
                if(s.split("=")[0].equals(feature.get(num)))
                    return null;
            }
        }
        //System.out.println(script.get(num) + " Spawning cmd "+num);
        String cmd = "bash ../"+script.get(num)+"/run.sh";
        for(int i = 1; i<args.length; i++) {
            switch(args[i].trim()) {
                case "JSON": cmd += " "+JSON+" ";
                    break;
                case "Face": cmd += " "+top+" "+right+" "+bottom+" "+left+" ";
                    break;
                case "Image": cmd += " "+Image+" ";
                    break;
                case "HaarFace": cmd += " "+haarTop+" "+haarRight+" "+haarBottom+" "+haarLeft;
                    break;
                case "LBPFace": cmd += " "+lbpTop+" "+lbpRight+" "+lbpBottom+" "+lbpLeft;
                    break;
                case "YAML": cmd += " " + Yaml + " ";
                    break;
                case "0":
                    zeroFeatures.put(feature.get(num),true);
                    return null;
                case "I":
                    break;
                default:
                    if(args[i].split("-")[0].equals("prefix"))
                        cmd += " "+ args[i].split("-")[1] + " ";
            }
        }
        return cmd;
    }
    public void executeCMD(String cmd, int num){
        try{
            long startTime = System.currentTimeMillis();
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String output;
            ArrayList<String> scriptFeatures = new ArrayList<String>();
            while((output = in.readLine()) != null) {
                scriptFeatures.add(output);
            }
            scriptToFeatures.get(num).put(script.get(num), scriptFeatures);
            //System.out.println(cmd);
            //System.out.println(System.currentTimeMillis()-startTime);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    public void parseArgs(String[] args) {
        for(int i = 1; i<args.length; i++) {
            switch(args[i].split("=")[0].trim()){
                case "JSON": JSON = args[i].split("=")[1];
                    break;
                case "Image": Image = args[i].split("=")[1];
                    break;
                case "YAML": Yaml = args[i].split("=")[1];
                    break;
            }
        }
    }
    public void getUtility(ArrayList<String> features){
        double utility = 0;
        double constants = 0;
        if(left == -1) {
                features.add("Utility=0");
                return;
        }
        for(String feature : features){
            String featureType = feature.split("=")[0];
            String featureVal = feature.split("=")[1];

            if(zeroFeatures.contains(feature))
                continue;

            if(featureType.equals("width") || featureType.equals("height")) {
                utility += .125*Double.parseDouble(featureVal);
                constants += .125;
            } else if(featureType.equals("centerx") || featureType.equals("centery")){
                double val = Double.parseDouble(featureVal);
                if(val >= .5)
                    utility += .1 * (1 - (2*(val-.5)));
                else
                    utility += .1 * (2*val);
                constants += .1;
            } else if(featureType.equals("GimbalPosition")){
                utility += .1 * Double.parseDouble(featureVal);
                constants += .1;
            } else if(featureType.substring(0, Math.min(featureType.length(), 10)).equals("Saturation")) {
                utility += .01 * Double.parseDouble(featureVal);
                constants += .01;
            } else if(featureType.equals("xResolution")) {
                utility += .025 * Double.parseDouble(featureVal)/4000;
                constants += .025;
            } else if(featureType.equals("yResolution")) {
                utility += .05 * Double.parseDouble(featureVal)/3000;
                constants += .05;
            } else if(featureType.substring(0,Math.min(featureType.length(), 8)).equals("Contrast")) {
                utility += .033 * Double.parseDouble(featureVal);
                constants += .033;
            } else if(featureType.equals("BrightnessOfMask")) {
                String bor = "";
                for(String s : features) {
                    if(s.split("=")[0].equals("BrightnessOfRest")) {
                        bor = s;
                        break;
                    }
                }
                if(bor.equals(""))
                    continue;
                utility += .08 * Math.abs(Double.parseDouble(featureVal) -
                                          Double.parseDouble(bor.split("=")[1]));
                constants += .08;
            } else if(featureType.equals("BrightnessTotal")) {
                utility += .03 * Double.parseDouble(featureVal);
                constants += .03;
            } else if(featureType.equals("time")) {
                double sec = Double.parseDouble(featureVal)*24.0;
                if(sec >= .5)
                    utility += .1 * (24 - 2.0*(sec - 12))/24.0;
                else
                    utility += .1 * (2.0*sec)/24.0;

                constants += .1;
            }
        }
        features.add("Utility="+(utility/constants));

    }
    public void runThread(ArrayList<String> lines, int num){
        for(String line : lines) {
            try {
                String cmd = getFeature(line, num);
                if(cmd != null) {
                    executeCMD(cmd, num);
                } else if(zeroFeatures.containsKey(feature)){
                    returnFeatures.get(num).add(feature+"=0");
                    continue;
                }

                if(script.get(num).equals("OpenCVHaar")){
                    for(String s :scriptToFeatures.get(num).get(script.get(num))) {
                        switch(s.split("=")[0]) {
                            case "HAARleft": haarLeft = Integer.parseInt(s.split("=")[1].trim()); break;
                            case "HAARtop": haarTop = Integer.parseInt(s.split("=")[1].trim()); break;
                            case "HAARbottom": haarBottom = Integer.parseInt(s.split("=")[1].trim()); break;
                            case "HAARright": haarRight = Integer.parseInt(s.split("=")[1].trim()); break;
                        }
                    }
                }
                if(script.get(num).equals("OpenCVLBP")){
                    for(String s : scriptToFeatures.get(num).get(script.get(num))) {
                        switch(s.split("=")[0]) {
                            case "LBPleft": lbpLeft = Integer.parseInt(s.split("=")[1].trim()); break;
                            case "LBPtop": lbpTop = Integer.parseInt(s.split("=")[1].trim()); break;
                            case "LBPbottom": lbpBottom = Integer.parseInt(s.split("=")[1].trim()); break;
                            case "LBPright": lbpRight = Integer.parseInt(s.split("=")[1].trim()); break;
                        }
                    }
                }
                for(String s : scriptToFeatures.get(num).get(script.get(num))){
                    if(s.split("=")[0].equals(feature.get(num))) {
                        returnFeatures.get(num).add(s);
                        switch(feature.get(num)) {
                            case "left": left = Integer.parseInt(s.split("=")[1].trim()); break;
                            case "right": right = Integer.parseInt(s.split("=")[1].trim()); break;
                            case "top": top = Integer.parseInt(s.split("=")[1].trim()); break;
                            case "bottom": bottom = Integer.parseInt(s.split("=")[1].trim()); break;
                        }
                        break;
                    }
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    public static ArrayList<String> reorg(ArrayList<ArrayList<String>> org, ArrayList<String> lines) {
        ArrayList<String> ret = new ArrayList<String>();
        ArrayList<String> flat = new ArrayList<String>();
        for(ArrayList<String> list : org) {
            for(String line : list) {
                flat.add(line);
            }
        }
        for(String orgLine : lines){
            String prefix = orgLine.split("=")[0].trim();
            for(String line : flat) {
                String orgPrefix = line.split("=")[0].trim();
                if(prefix.equals(orgPrefix)) {
                    //System.out.println(orgPrefix + " "+ prefix);
                    ret.add(line);
                    break;
                }
            }
        }

        return ret;
    }
    public static void main(String args[]){
        Parser p= new Parser();
        p.parseArgs(args);
        p.scriptToFeatures = new ArrayList<Hashtable<String, ArrayList<String>>>();
        p.returnFeatures = new ArrayList<ArrayList<String>>();
        p.script = new ArrayList<String>();
        p.feature = new ArrayList<String>();
        try {
            File file = new File(args[0]);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            //FileWriter fw = new FileWriter(args[1], true);
            //BufferedWriter bw = new BufferedWriter(fw);
            //PrintWriter out = new PrintWriter(bw);

            String l;
            ArrayList<ArrayList<String>> threadLines = new ArrayList<ArrayList<String>>();
            ArrayList<String> lines = new ArrayList<String>();
            while((l = bufferedReader.readLine()) != null){
                String terms[] = l.split(",");
                int tNum = Integer.parseInt(terms[terms.length-1].substring(1));
                //System.out.println(tNum);
                if(tNum > threadLines.size()) {
                    threadLines.add(new ArrayList<String>());
                    p.scriptToFeatures.add(new Hashtable<String, ArrayList<String>>());
                    p.returnFeatures.add(new ArrayList<String>());
                    p.script.add("");
                    p.feature.add("");
                }
                threadLines.get(tNum-1).add(l);
                lines.add(l);
            }

            //First 3 are faces, which need to be run before other features
            //This should be fixed so any number of threads can be faces
            ArrayList<Thread> threads = new ArrayList<Thread>();
            Runnable r = new MyRunnable(p, threadLines.get(0), 0);
            Thread t = new Thread(r);
            threads.add(t);
            t.start();

            Runnable r2 = new MyRunnable(p, threadLines.get(1), 1);
            t = new Thread(r2);
            threads.add(t);
            t.start();

            Runnable r3 = new MyRunnable(p, threadLines.get(2), 2);
            t = new Thread(r3);
            threads.add(t);
            t.start();

            threads.get(0).join();
            threads.get(1).join();
            threads.get(2).join();

            //System.out.println("joined");
            for(int i = 3; i<threadLines.size(); i++){
                //System.out.println(i + " " +threadLines.get(i).get(0));
                t = new Thread(new MyRunnable(p, threadLines.get(i), i));
                threads.add(t);
                t.start();
            }

            for(Thread thr : threads){
                thr.join();
            }

            ArrayList<String> output = reorg(p.returnFeatures, lines);

            p.getUtility(output);
            System.out.println(output);
            System.out.println(output.size());
            //System.out.println(Arrays.toString(p.returnFeatures.toArray()));

        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
class MyRunnable implements Runnable {
    Parser p;
    ArrayList<String> l;
    int tnum;
    public MyRunnable(Parser p, ArrayList<String> l, int tnum) {
        this.p = p;
        this.l = l;
        this.tnum = tnum;
    }
    public void run() {
        //System.out.println("Thread"+tnum+" spawned "+l.get(0));
        p.runThread(l, tnum);
    }
}
