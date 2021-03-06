package org.jaewanyun.plasmidplanner;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Enzyme {

	private String enzymeName; // Stores the name of the enzyme
	private String description; // Stores enzyme description (e.g. Timesaver, CpG, dcm, etc)
	private String originalSequence;
	private String suppliedBuffer;
	private String activity11;
	private String activity21;
	private String activity31;
	private String activityCutsmart;
	private String heatInactivation;
	private String incubationTemp;
	private String diluent;
	private String origRecognitionSequence; // Stores the recognition sequence of this enzyme
	private String compRecognitionSequence; // Stores the complement of the recognition sequence
	private int origCut; // Stores the cutting index from the beginning of origRecognitionSequence
	private int compCut; // Stores the cutting index from the beginning of compRecognitionSequence
	private String origRecognitionSequenceRegex;
	private String compRecognitionSequenceRegex;
	private Pattern origCompile;
	private Pattern compCompile;


	private Enzyme(String enzymeName, String description, String originalSequence, String suppliedBuffer, String activity11, String activity21, String activity31, String activityCutsmart,
			String heatInactivation, String incubationTemp, String diluent,
			String origRecognitionSequence, String compRecognitionSequence, int origCut, int compCut) {
		this.enzymeName = enzymeName;
		this.description = description;
		this.originalSequence = originalSequence;
		this.suppliedBuffer = suppliedBuffer;
		this.activity11 = activity11;
		this.activity21 = activity21;
		this.activity31 = activity31;
		this.activityCutsmart = activityCutsmart;
		this.heatInactivation = heatInactivation;
		this.incubationTemp = incubationTemp;
		this.diluent = diluent;
		this.origRecognitionSequence = origRecognitionSequence;
		this.compRecognitionSequence = compRecognitionSequence;
		this.origCut = origCut;
		this.compCut = compCut;
		this.origRecognitionSequenceRegex = Utility.enumerate(origRecognitionSequence);
		this.compRecognitionSequenceRegex = Utility.enumerate(compRecognitionSequence);

		this.origCompile = Pattern.compile(this.origRecognitionSequenceRegex, Pattern.UNICODE_CASE);
		this.compCompile = Pattern.compile(this.compRecognitionSequenceRegex, Pattern.UNICODE_CASE);
	}


	@Override
	public String toString() {
		return ("Enzyme: " + this.enzymeName +
				"\tInfo:" + this.description +
				"\tRecognition Seq.: " + this.originalSequence +
				"\tSupplied Buffer: " + this.suppliedBuffer +
				"\t1.1 Buffer: " + this.activity11 + "%" +
				"\t2.1 Buffer: " + this.activity21 + "%" +
				"\t3.1 Buffer: " + this.activity31 + "%" +
				"\tCutSmart Buffer: " + this.activityCutsmart + "%" +
				"\tInactivation Temp.: " + this.heatInactivation +
				"\tIncubation Temp.: " + this.incubationTemp +
				"\tDiluent: " + this.diluent);
	}

	/*
	 * Digests and modifies the plasmid passed in
	 */
	public void digest(Plasmid plasmid) {
		Matcher origMatcher = this.origCompile.matcher(plasmid.getSequence());
		Matcher compMatcher = this.compCompile.matcher(plasmid.getSequence());

		// Get these to modify them
		HashMap<Integer, HashSet<String>> locationAndEnzyme;
		HashMap<Integer, HashSet<Overhang>> locationAndOverhang;
		HashMap<String, HashSet<Integer>> enzymeAndLocation;
		HashMap<Overhang, HashSet<String>> overhangAndEnzyme;
		HashMap<String, HashSet<Overhang>> enzymeAndOverhang;
		HashMap<String, Overhang> enzymeAndLocationAndOverhang;

		locationAndEnzyme = plasmid.getLocationAndEnzyme();
		locationAndOverhang = plasmid.getLocationAndOverhang();
		enzymeAndLocation = plasmid.getEnzymeAndLocation();
		overhangAndEnzyme = plasmid.getOverhangAndEnzyme();
		enzymeAndOverhang = plasmid.getEnzymeAndOverhang();
		enzymeAndLocationAndOverhang = plasmid.getEnzymeAndLocationAndOverhang();

		int position = 0;
		while(origMatcher.find(position)) {
			int start = origMatcher.start();
			int overhangStart = start + Math.min(this.origCut, this.compCut);
			int overhangEnd = start + Math.max(this.origCut, this.compCut);
			String plasmidSequence = plasmid.getSequence();
			String origOverhangSequence = "";
			if(overhangEnd < plasmid.getLength()) {
				for(int j = overhangStart; j < overhangEnd; j++) {
					origOverhangSequence += plasmidSequence.charAt(j);
				}
			}

			synchronized (plasmid) {
				// Check if the map contains the cut location and append names
				if(locationAndEnzyme.containsKey(overhangStart)) {
					HashSet<String> got = locationAndEnzyme.get(overhangStart);
					got.add(this.enzymeName);
				}
				else {
					HashSet<String> enzymes = new HashSet<>();
					enzymes.add(this.enzymeName);
					locationAndEnzyme.put(overhangStart, enzymes);
				}

				// Check if the map contains the cut location and append overhangs
				if(locationAndOverhang.containsKey(overhangStart)) {
					HashSet<Overhang> got = locationAndOverhang.get(overhangStart);
					got.add(Plasmid.getOverhang(origOverhangSequence, (this.origCut > this.compCut)));
				}
				else {
					HashSet<Overhang> overhangs = new HashSet<>();
					overhangs.add(Plasmid.getOverhang(origOverhangSequence, (this.origCut > this.compCut)));
					locationAndOverhang.put(overhangStart, overhangs);
				}

				// Check if the map contains the enzyme and append the locations
				if(enzymeAndLocation.containsKey(this.enzymeName)) {
					HashSet<Integer> got = enzymeAndLocation.get(this.enzymeName);
					got.add(overhangStart);
				}
				else {
					HashSet<Integer> locations = new HashSet<>();
					locations.add(overhangStart);
					enzymeAndLocation.put(this.enzymeName, locations);
				}

				// Check if the map contains the overhang and append the enzymes
				Overhang overhang = Plasmid.getOverhang(origOverhangSequence, (this.origCut > this.compCut));
				if(overhangAndEnzyme.containsKey(overhang)) {
					HashSet<String> got = overhangAndEnzyme.get(overhang);
					got.add(this.enzymeName);
				}
				else {
					HashSet<String> names = new HashSet<>();
					names.add(this.enzymeName);
					overhangAndEnzyme.put(overhang, names);
				}

				// Check if the map contains the enzyme and append the overhangs
				if(enzymeAndOverhang.containsKey(this.enzymeName)) {
					HashSet<Overhang> got = enzymeAndOverhang.get(this.enzymeName);
					got.add(Plasmid.getOverhang(origOverhangSequence, (this.origCut > this.compCut)));
				}
				else {
					HashSet<Overhang> overhangs = new HashSet<>();
					overhangs.add(Plasmid.getOverhang(origOverhangSequence, (this.origCut > this.compCut)));
					enzymeAndOverhang.put(this.enzymeName, overhangs);
				}

				// Check if the map contains the enzyme + location and append the overhangs
				enzymeAndLocationAndOverhang.put(this.enzymeName + Integer.toString(overhangStart), Plasmid.getOverhang(origOverhangSequence, (this.origCut > this.compCut)));

				position++;
			}
		}

		if(!this.origRecognitionSequence.equals(this.compRecognitionSequence)) {
			position = 0;
			while(compMatcher.find(position)) {
				int start = compMatcher.start();
				int overhangStart = start + Math.min(this.origCut, this.compCut);
				int overhangEnd = start + Math.max(this.origCut, this.compCut);
				String plasmidSequence = plasmid.getSequence();
				String compOverhangSequence = "";
				if(overhangEnd < plasmid.getLength()) {
					for(int j = overhangStart; j < overhangEnd; j++) {
						compOverhangSequence += plasmidSequence.charAt(j);
					}
				}

				synchronized (plasmid) {
					// Check if the map contains the cut location and append names
					if(locationAndEnzyme.containsKey(overhangStart)) {
						HashSet<String> got = locationAndEnzyme.get(overhangStart);
						got.add(this.enzymeName);
					}
					else {
						HashSet<String> enzymes = new HashSet<>();
						enzymes.add(this.enzymeName);
						locationAndEnzyme.put(overhangStart, enzymes);
					}

					// Check if the map contains the cut location and append overhangs
					if(locationAndOverhang.containsKey(overhangStart)) {
						HashSet<Overhang> got = locationAndOverhang.get(overhangStart);
						got.add(Plasmid.getOverhang(compOverhangSequence, (this.origCut > this.compCut)));

					}
					else {
						HashSet<Overhang> overhangs = new HashSet<>();
						overhangs.add(Plasmid.getOverhang(compOverhangSequence, (this.origCut > this.compCut)));
						locationAndOverhang.put(overhangStart, overhangs);
					}

					// Check if the map contains the enzyme and append the locations
					if(enzymeAndLocation.containsKey(this.enzymeName)) {
						HashSet<Integer> got = enzymeAndLocation.get(this.enzymeName);
						got.add(overhangStart);
					}
					else {
						HashSet<Integer> locations = new HashSet<>();
						locations.add(overhangStart);
						enzymeAndLocation.put(this.enzymeName, locations);
					}

					// Check if the map contains the overhang and append the enzymes
					Overhang overhang = Plasmid.getOverhang(compOverhangSequence, (this.origCut > this.compCut));
					if(overhangAndEnzyme.containsKey(overhang)) {
						HashSet<String> got = overhangAndEnzyme.get(overhang);
						got.add(this.enzymeName);
					}
					else {
						HashSet<String> names = new HashSet<>();
						names.add(this.enzymeName);
						overhangAndEnzyme.put(overhang, names);
					}

					// Check if the map contains the enzyme and append the overhangs
					if(enzymeAndOverhang.containsKey(this.enzymeName)) {
						HashSet<Overhang> got = enzymeAndOverhang.get(this.enzymeName);
						got.add(Plasmid.getOverhang(compOverhangSequence, (this.origCut > this.compCut)));
					}
					else {
						HashSet<Overhang> overhangs = new HashSet<>();
						overhangs.add(Plasmid.getOverhang(compOverhangSequence, (this.origCut > this.compCut)));
						enzymeAndOverhang.put(this.enzymeName, overhangs);
					}

					// No need to check as there can only be one answer
					enzymeAndLocationAndOverhang.put(this.enzymeName + Integer.toString(overhangStart), Plasmid.getOverhang(compOverhangSequence, (this.origCut > this.compCut)));
				}
				position++;
			}
		}
	}


	public String getEnzymeName() {
		return this.enzymeName;
	}

	public String getOrigRecognitionSequence() {
		return this.origRecognitionSequence;
	}

	public String getCompRecognitionSequence() {
		return this.compRecognitionSequence;
	}

	public int getOrigCut() {
		return this.origCut;
	}

	public int getCompCut() {
		return this.compCut;
	}


	/*
	 * Parses into an array of Enzyme using the contents of a file
	 */
	public static Enzyme[] importFromFile(InputStream fileLocation) {
		String[] unparsedStrings = Utility.readFile(fileLocation);
		ArrayList<Enzyme> enzymes = new ArrayList<>();
		// Parse recognition sequence
		for(int j = 0; j < unparsedStrings.length; j++) {
			Enzyme enz = parse(unparsedStrings[j]);
			if(enz != null)
				enzymes.add(enz);
		}
		return enzymes.toArray(new Enzyme[enzymes.size()]);
	}

	/*
	 * Parses into an instance of this using a line read from file
	 */
	private static Enzyme parse(String unparsed) {
		int origCut = 0;
		int compCut = 0;
		boolean valid = false;
		String origRecognitionSequence = "";

		int sizeWithoutParenthesis;
		String[] cuts = unparsed.split("\t");

		if(cuts[0].contains("RE-Mix") || cuts[0].contains("-HF")) // Ignore duplicates with fancy names
			return null;

		String builder = "";
		if(cuts[0].contains("(")) {
			for(int j = 0; j < cuts[0].length(); j++) {
				if(cuts[0].charAt(j) == '(')
					break;
				builder += cuts[0].charAt(j);
			}
			cuts[0] = builder;
		}

		// Get rid of slashes when importing recognition sequences
		String recognitionSequence = cuts[2].toLowerCase();
		String nucleotides = "atgcwsmkrybdhvn"; // Append only the letters found in here
		for(int k = 0; k < recognitionSequence.length(); k++) {
			if(recognitionSequence.charAt(k) == '/') {
				origCut = k;
				compCut = (recognitionSequence.length() - 1) - k;
				valid = true;
			}
			if(recognitionSequence.charAt(k) == '(' && k != 0) {
				k++; // Skip parenthesis
				sizeWithoutParenthesis = k - 1;
				String numberBuilder1 = "";
				String numberBuilder2 = "";
				while(recognitionSequence.charAt(k) != '/') {
					numberBuilder1 += recognitionSequence.charAt(k);
					k++;
					valid = true;
				}
				k++; // Skip slash
				while(recognitionSequence.charAt(k) != ')' && k+1 < recognitionSequence.length()) {
					numberBuilder2 += recognitionSequence.charAt(k);
					k++;
				}
				origCut = sizeWithoutParenthesis + Integer.parseInt(numberBuilder1);
				compCut = sizeWithoutParenthesis + Integer.parseInt(numberBuilder2);
			}
			if(!nucleotides.contains(Character.toString(recognitionSequence.charAt(k))))
				continue;
			origRecognitionSequence += recognitionSequence.charAt(k);
		}
		if(valid) {
			String compRecognitionSequence = Utility.complementSequence(origRecognitionSequence, true);
			return new Enzyme(cuts[0], cuts[1], cuts[2], cuts[3], cuts[4], cuts[5], cuts[6], cuts[7], cuts[8], cuts[9], cuts[10], origRecognitionSequence, compRecognitionSequence, origCut, compCut);
		}
		return null;
	}
}
