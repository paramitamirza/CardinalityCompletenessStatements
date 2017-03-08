package de.mpg.mpiinf.cardinality.autoextraction;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class PreprocessingConcurrent {
	
	public static void main(String[] args) throws Exception {
		
		Options options = getPreprocessingOptions();

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
            
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			formatter.printHelp("RelationCardinalityExtraction: Preprocessing", options);

			System.exit(1);
			return;
		}
		
//		String inputCsvFile = "./data/auto_extraction/wikidata_sample.csv";
		String inputCsvFile = cmd.getOptionValue("input");
		
		//If input CSV file doesn't have Wikipedia labels for each Wikidata ID
		if (cmd.hasOption("l")) {
			if (cmd.hasOption("w")) {
				String wikipediaLinkFile = cmd.getOptionValue("wikiurl");
				AddWikipediaTitle addWikiTitle = new AddWikipediaTitle(inputCsvFile, wikipediaLinkFile);
				
				int nRandom = 0;
				if (cmd.hasOption("n")) nRandom = Integer.parseInt(cmd.getOptionValue("randomize"));
				addWikiTitle.append(nRandom);
			} else {
				System.err.println("Mapping file between Wikipedia English URL and Wikidata entity (.txt.gz) is missing!");
				System.err.println("-- Specify -w [wiki-mapping file (.txt.gz) path]");

	            System.exit(1);
	            return;
			}
		}
		
		//Extract Wikipedia sentences (containing numbers) per Wikidata instance
		if (cmd.hasOption("s")) {
			String outputJsonFile = inputCsvFile.replace(".csv", ".jsonl.gz");
			SentenceExtractionFromWikipedia sentExtraction = new SentenceExtractionFromWikipedia(inputCsvFile, outputJsonFile);
			sentExtraction.extractSentences();
		}
		
		//Generate feature file (in column format) for CRF++
		if (cmd.hasOption("f")) {
			String inputJsonFile = inputCsvFile.replace(".csv", ".jsonl.gz");
			String inputRandomCsvFile = null;
			
			if (cmd.hasOption("n")) {
				int nRandom = Integer.parseInt(cmd.getOptionValue("randomize"));
				inputRandomCsvFile = inputCsvFile.replace(".csv", "_random"+nRandom+".csv");
			} else {
				if (cmd.hasOption("r")) {
					inputRandomCsvFile = cmd.getOptionValue("random");
				}
			}
			String relName = cmd.getOptionValue("relname");
			
			String dirFeature = null;
			if (cmd.hasOption("o")) {
				dirFeature = cmd.getOptionValue("output");
			} 
			
			if (inputRandomCsvFile == null) {
				System.err.println("Input random file (.csv) path for testing is missing!");
				System.err.println("-- Either specify -n [num_random] or -r [random file (.csv) path]");

	            System.exit(1);
	            return;
	            
			} else if (dirFeature == null) {
				System.err.println("Output directory of feature files (in column format) for CRF++ is missing!");
				System.err.println("-- Specify -o [dir_path]");

	            System.exit(1);
	            return;
	            
			} else {
				FeatureExtractionConcurrent featExtraction = new FeatureExtractionConcurrent(inputCsvFile, inputRandomCsvFile, relName, dirFeature);
				
				boolean nummod = cmd.hasOption("d");
				boolean compositional = cmd.hasOption("c");
				int threshold = 0;
				if (cmd.hasOption("t")) threshold = Integer.parseInt(cmd.getOptionValue("threshold"));
				featExtraction.run(nummod, compositional, threshold);
			}
		}
		
	}
	
	public static Options getPreprocessingOptions() {
		Options options = new Options();
		
		Option input = new Option("i", "input", true, "Input file (.csv) path");
		input.setRequired(true);
		options.addOption(input);
		
		Option relName = new Option("p", "relname", true, "Property/relation name");
		relName.setRequired(true);
		options.addOption(relName);
		
		Option addLinks = new Option("l", "links", false, "Add Wikipedia title page for WikiURL");
		addLinks.setRequired(false);
		options.addOption(addLinks);
		
		Option enLinks = new Option("w", "wikiurl", true, "Wikipedia English URL of Wikidata entity");
		enLinks.setRequired(false);
		options.addOption(enLinks);
		
		Option random = new Option("n", "randomize", true, "Generate n random instances for testing");
		random.setRequired(false);
		options.addOption(random);
		
		Option randomFile = new Option("r", "random", true, "Input random file (.csv) path for testing");
		randomFile.setRequired(false);
		options.addOption(randomFile);
		
		Option extractSent = new Option("s", "sentences", false, "Extract Wikipedia sentences (containing numbers) per Wikidata instance");
		extractSent.setRequired(false);
		options.addOption(extractSent);
		
		Option extractFeature = new Option("f", "features", false, "Generate feature file (in column format) for CRF++");
		extractFeature.setRequired(false);
		options.addOption(extractFeature);
		
		Option output = new Option("o", "output", true, "Output directory of feature files (in column format) for CRF++");
		output.setRequired(false);
		options.addOption(output);
		
		Option nummod = new Option("d", "nummod", false, "Only if dependency label is 'nummod' to be labelled as positive examples");
		nummod.setRequired(false);
		options.addOption(nummod);
		
		Option compositional = new Option("c", "compositional", false, "Label compositional numbers as true examples");
		compositional.setRequired(false);
		options.addOption(compositional);
		
		Option threshold = new Option("t", "threshold", true, "Threshold for number of triples to be labelled as positive examples");
		threshold.setRequired(false);
		options.addOption(threshold);
		
		return options;
	}

}