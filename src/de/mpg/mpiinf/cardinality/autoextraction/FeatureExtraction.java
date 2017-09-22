package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class FeatureExtraction {
	
	private String inputCsvFile = "./data/example/wikidata_sample.csv";
	private String inputRandomCsvFile = "./data/example/wikidata_sample_random10.csv";
	private String relName = "sample";
	private String dirFeature = "./data/example/";
	
	public FeatureExtraction() {
		
	}
	
	public FeatureExtraction(String inputCsvFilePath, String relationName, String dirOutput) {
		this.setInputCsvFile(inputCsvFilePath);
		this.setInputRandomCsvFile("");
		this.setRelName(relationName);
		this.setDirFeature(dirOutput);
	}
	
	public FeatureExtraction(String inputCsvFilePath, int nRandom, String relationName, String dirOutput) throws IOException {
		this.setInputCsvFile(inputCsvFilePath);
		this.generateRandomInstances(nRandom);
		this.setRelName(relationName);
		this.setDirFeature(dirOutput);
	}
	
	public FeatureExtraction(String inputCsvFilePath, String inputRandomCsvFilePath, String relationName, String dirOutput) {
		this.setInputCsvFile(inputCsvFilePath);
		this.setInputRandomCsvFile(inputRandomCsvFilePath);
		this.setRelName(relationName);
		this.setDirFeature(dirOutput);
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		FeatureExtraction featExtraction;
		if (args.length < 4) {
			featExtraction = new FeatureExtraction();
		} else {
			featExtraction = new FeatureExtraction(args[0], args[1], args[2], args[3]);
		}
		
		WikipediaArticle wiki = new WikipediaArticle();
		featExtraction.run(wiki, true, false, 0, false, false, false, false, -99, 0, (float) 1.0);
	}
	
	public void run(WikipediaArticle wiki, boolean nummod, boolean compositional, float threshold,
			boolean transform, boolean transformZero, boolean transformOne, 
			boolean ignoreHigher, int ignoreHigherLess, 
			int ignoreFreq,float topPopular) throws IOException, InterruptedException {
		
		long startTime = System.currentTimeMillis();
		System.out.print("Generate feature file (in column format) for CRF++... ");
		
		removeOldFeatureFiles();
		
		List<String> testInstances = readRandomInstances(getInputRandomCsvFile());
		String line;
		String wikidataId = "", count = "", freqNum = "", quarter = "", countDist = "";
		Integer curId;
		boolean training, isIgnoreFreq;
		
		//******* Reading maximum triple count in the dataset *******//
		
		int maxCount = 0, tripleCount = 0;;
		BufferedReader brpre = new BufferedReader(new FileReader(getInputCsvFile()));
		line = brpre.readLine();
		while (line != null) {
			tripleCount = Integer.parseInt(line.split(",")[1]);
			if (tripleCount >= maxCount) maxCount = tripleCount;
			
			line = brpre.readLine();
		}
		brpre.close();
		
		BufferedReader br = new BufferedReader(new FileReader(getInputCsvFile()));
		
		int numTrain = ReadFromFile.countLines(this.getInputCsvFile()) - ReadFromFile.countLines(this.getInputRandomCsvFile());
		int maxNumTrain = Math.round(topPopular * numTrain);
		int idxTrain = 0;
		
		line = br.readLine();
		
		while (line != null) {
			wikidataId = line.split(",")[0];
	        count = line.split(",")[1];
	        curId = Integer.parseInt(line.split(",")[2]);
	        
	        countDist = line.split(",")[5];
	        quarter = line.split(",")[6];
	        
	        isIgnoreFreq = false;
	        if (ignoreFreq >= 0) {
	        	isIgnoreFreq = true;
	        	if (ignoreFreq == 0) freqNum = line.split(",")[7];
	            else if (ignoreFreq == 1) freqNum = line.split(",")[8];
	            else if (ignoreFreq == 2) freqNum = line.split(",")[9];
	            else if (ignoreFreq == 3) freqNum = line.split(",")[10];
	            else if (ignoreFreq == 4) freqNum = line.split(",")[11];
	        }   
	        
	        training = true;
	        if (testInstances.contains(wikidataId)) {
				training = false;
			} 
	        
	        training = true;
	        if (testInstances.contains(wikidataId)) {
				training = false;
			} 
	        if (training && (idxTrain < maxNumTrain)) {
		        GenerateFeatures ext = new GenerateFeatures(getDirFeature(), getRelName(),
		        		wiki, wikidataId, count, curId, freqNum,
		        		training,
		        		nummod, compositional, 
		        		threshold, countDist,
		        		transform, transformZero, transformOne,
		        		ignoreHigher, ignoreHigherLess, isIgnoreFreq, maxCount);
				ext.run();
				idxTrain ++;
	        } else {
	        	GenerateFeatures ext = new GenerateFeatures(getDirFeature(), getRelName(),
		        		wiki, wikidataId, count, curId, freqNum,
		        		training,
		        		nummod, compositional, 
		        		threshold, countDist,
		        		transform, transformZero, transformOne,
		        		ignoreHigher, ignoreHigherLess, isIgnoreFreq, maxCount);
				ext.run();
	        }
             
            line = br.readLine();
		}
		
		long endTime   = System.currentTimeMillis();
		float totalTime = (endTime - startTime)/(float)1000;
		System.out.println("done [ " + totalTime + " sec].");
		
		br.close();
	}
	
	public void ensureDirectory(File dir) {
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
	
	public void removeOldFeatureFiles() throws IOException {
		ensureDirectory(new File(this.getDirFeature()));
		File train = new File(this.getDirFeature() + this.getRelName() + "_train_cardinality.data");
		File test = new File(this.getDirFeature() + this.getRelName() + "_test_cardinality.data");
		Files.deleteIfExists(train.toPath());
		Files.deleteIfExists(test.toPath());
	}
	
	public List<String> readRandomInstances(String inputFile) throws IOException {
		System.out.println("Read random instances...");
		List<String> randomInstances = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(inputFile));
		String line = br.readLine();		
		while (line != null) {
			randomInstances.add(line.split(",")[0]);
			line = br.readLine();
		}
		br.close();
		return randomInstances;
	}
	
	public void generateRandomInstances(int nRandom) throws IOException {
		this.setInputRandomCsvFile(this.getInputCsvFile().replace(".csv", "_random"+nRandom+".csv"));
		BufferedReader br = new BufferedReader(new FileReader(this.getInputCsvFile()));
		BufferedWriter bwr = new BufferedWriter(new FileWriter(this.getInputRandomCsvFile()));
		List<Integer> randomList = new ArrayList<Integer>();
		if (nRandom > 0) {
			LineNumberReader lnr = new LineNumberReader(new FileReader(this.getInputCsvFile()));
			Stack<Integer> randomPool = new Stack<Integer>();
			int linenumber = 0;
			while (lnr.readLine() != null) {
				randomPool.add(linenumber);
				linenumber++;
			}
			Collections.shuffle(randomPool);
			randomList = randomPool.subList(0, nRandom);
			lnr.close();
		}
		
		String eid = "", count = "";
		String line = br.readLine();	
		int n = 0;
		while (line != null) {
			eid = line.split(",")[0];
			count = line.split(",")[1];
				
			if (randomList.contains(n)) {
				bwr.write(eid + "," + count);
				bwr.newLine();
			}
			
			line = br.readLine();
			n ++;
		}
		br.close();
		bwr.close();
	}

	public String getInputCsvFile() {
		return inputCsvFile;
	}

	public void setInputCsvFile(String inputCsvFile) {
		this.inputCsvFile = inputCsvFile;
	}

	public String getInputRandomCsvFile() {
		return inputRandomCsvFile;
	}

	public void setInputRandomCsvFile(String inputRandomCsvFile) {
		this.inputRandomCsvFile = inputRandomCsvFile;
	}
	
	public String getRelName() {
		return relName;
	}

	public void setRelName(String relationName) {
		this.relName = relationName;
	}
	
	public String getDirFeature() {
		return dirFeature;
	}

	public void setDirFeature(String dirFeature) {
		this.dirFeature = dirFeature;
	}
}
