import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * Disassembler for Legv8 assembly code
 *
 * @author rmbanks
 * @author nbc
 * @author hetik
 */
public class Disassembler {
	private static ArrayList<String> fileData = new ArrayList<>();
	private static ArrayList<Instruction> instructions = new ArrayList<>();

	public static void main(String[] args) throws IOException {
		// Parse the bytes of the input file into big endian binary
		if (args != null && args.length > 0) {
			parseFile(args[0]);
		}
		// Parse the list of file data into instructions
		for (int i = 0; i < fileData.size(); i++) {
			instructions.add(parseInstruction(fileData.get(i)));
		}
		// Parse through the instructions to set the labels correctly
		for (int i = 0; i < instructions.size(); i++) {
			int labelCount = 0;
			if (instructions.get(i).branch) {
				String[] splitInstruction = instructions.get(i).instruction.split(" ");
				labelCount = Integer.parseInt(splitInstruction[1]);
				if (splitInstruction[0].contains("CBZ") || splitInstruction[0].contains("CBNZ")) {
					labelCount = Integer.parseInt(splitInstruction[2]);
				}
				instructions.get(i + labelCount).setPrintLabel(true);
			}
		}
		// Loops through instruction list to set the label on instructions that need
		// labels above them
		int count = 0;
		for (int i = 0; i < instructions.size(); i++) {
			if (instructions.get(i).isPrintLabel()) {
				String label = "label_" + count + ":";
				instructions.get(i).setLabel(label);
				count++;
			}
		}
		// Print everything
		for (int i = 0; i < instructions.size(); i++) {
			if (instructions.get(i).isBranch()) {
				if (instructions.get(i).isPrintLabel()) {
					System.out.println(instructions.get(i).getLabel());
				}
				String[] splitInstruction = instructions.get(i).getInstruction().split(" ");
				String toPrint = splitInstruction[0] + " "
						+ instructions.get(i + Integer.parseInt(splitInstruction[1])).getLabel();
				if (splitInstruction[0].contains("CBZ") || splitInstruction[0].contains("CBNZ")) {
					toPrint = splitInstruction[0] + " " + splitInstruction[1] + " "
							+ instructions.get(i + Integer.parseInt(splitInstruction[2])).getLabel();
				}
				System.out.println(toPrint);
			} else if (instructions.get(i).isPrintLabel()) {
				System.out.println(instructions.get(i).getLabel());
				System.out.println(instructions.get(i).getInstruction());
			} else {
				System.out.println(instructions.get(i).getInstruction());
			}
		}
	}

	/**
	 * Parses the file given on the command line
	 *
	 * @param fileName fileName
	 * @throws IOException
	 */
	public static void parseFile(String fileName) throws IOException {
		File file = new File(fileName);
		byte[] bytes = Files.readAllBytes(file.toPath());
		String instruction = "";
		for (int i = 0; i < bytes.length; i++) {
			StringBuilder stringBuilder = new StringBuilder("00000000");
			for (int bit = 0; bit < 8; bit++) {
				if (((bytes[i] >> bit) & 1) > 0) {
					stringBuilder.setCharAt(7 - bit, '1');
				}
			}
			instruction += stringBuilder.toString();
			if (instruction.length() == 32) {
				fileData.add(instruction);
				instruction = "";
			}
		}
	}

	/**
	 * Converts a binary number into decimal form
	 *
	 * @param binary binary
	 * @return decimal
	 */
	public static int binaryToDecimal(String binary) {
		char[] chars = binary.toCharArray();
		int decimal;
		if (chars[0] == '1') {
			String twosCompliment = twosCompliment(binary);
			decimal = Integer.parseInt(twosCompliment, 2);
			decimal = decimal * -1;
		} else {
			decimal = Integer.parseInt(binary, 2);
		}
		return decimal;
	}

	/**
	 * Calculates the Two's Compliment of a binary number
	 *
	 * @param binary binary
	 * @return Two's Compliment
	 */
	public static String twosCompliment(String binary) {
		String number = "";
		for (int i = 0; i < binary.length(); i++) {
			number += flipBit(binary.charAt(i));
		}
		StringBuilder stringBuilder = new StringBuilder(number);
		boolean zero = false;
		for (int i = number.length() - 1; i > 0; i--) {
			if (number.charAt(i) == '1') {
				stringBuilder.setCharAt(i, '0');
			} else {
				stringBuilder.setCharAt(i, '1');
				zero = true;
				break;
			}
		}
		if (!zero) {
			stringBuilder = new StringBuilder("10000000");
		}
		return stringBuilder.toString();
	}

	/**
	 * Flips the bit of a char
	 *
	 * @param bit bit
	 * @return flipped bit
	 */
	public static char flipBit(char bit) {
		if (bit == '0') {
			return '1';
		}
		return '0';
	}

	/*
	 * Disassembler fully support the following set of LEGv8 instructions:
	 * 
	 * ADD -
	 * 
	 * ADDI -
	 * 
	 * AND -
	 * 
	 * ANDI -
	 * 
	 * B -
	 * 
	 * B.cond: This is a CB instruction in which the Rt field is not a register, but
	 * a code that indicates the condition extension. These have the values (base
	 * 16):
	 * 
	 * 0: EQ -
	 * 
	 * 1: NE -
	 * 
	 * 2: HS -
	 * 
	 * 3: LO -
	 * 
	 * 4: MI -
	 * 
	 * 5: PL -
	 * 
	 * 6: VS -
	 * 
	 * 7: VC -
	 * 
	 * 8: HI -
	 * 
	 * 9: LS -
	 * 
	 * a: GE -
	 * 
	 * b: LT -
	 * 
	 * c: GT -
	 * 
	 * d: LE -
	 * 
	 * BL -
	 * 
	 * BR - : The branch target is encoded in the Rn field.
	 * 
	 * CBNZ -
	 * 
	 * CBZ -
	 * 
	 * EOR -
	 * 
	 * EORI -
	 * 
	 * LDUR -
	 * 
	 * LSL - : This instruction uses the shamt field to encode the shift
	 * amount,Â while Rm is unused.
	 * 
	 * LSR - : Same as LSL.
	 * 
	 * ORR -
	 * 
	 * ORRI -
	 * 
	 * STUR -
	 * 
	 * SUB -
	 * 
	 * SUBI -
	 * 
	 * SUBIS -
	 * 
	 * SUBS -
	 * 
	 * MUL -
	 * 
	 * PRNT - : This is an added instruction (part of our emulator, but not part of
	 * LEG or ARM) that prints a register name and its contents in hex and decimal.
	 * This is an R instruction. The opcode is 11111111101. The register is given in
	 * the Rd field.
	 * 
	 * PRNL - : This is an added instruction that prints a blank line. This is an R
	 * instruction. The opcode is 11111111100.
	 * 
	 * DUMP - : This is an added instruction that displays the contents of all
	 * registers and memory, as well as the disassembled program. This is an R
	 * instruction. The opcode is 11111111110.
	 * 
	 * HALT - : This is an added instruction that triggers a DUMP and terminates the
	 * emulator. This is an R instruction. The opcode is 11111111111
	 */

	// given substring of a 32-bit instruction excluding the 11 bit opcode so its
	// 11-32

	/**
	 * Gets the assembly instruction, excluding the opcode, for an R-type
	 * instruction
	 *
	 * @param subInstruction substring of the Instruction 11-32 bits
	 * @param shift          shift
	 * @return instruction
	 */
	public static String getRInstruction(String subInstruction, boolean shift) {
		int Rm = Integer.parseInt(subInstruction.substring(0, 5), 2);
		int shamt = Integer.parseInt(subInstruction.substring(5, 11), 2);
		int Rn = Integer.parseInt(subInstruction.substring(11, 16), 2);
		int Rd = Integer.parseInt(subInstruction.substring(16), 2);

		if (shift) {
			return " X" + Rd + ", " + "X" + Rn + ", " + "#" + shamt;
		}
		return " X" + Rd + ", " + "X" + Rn + ", " + "X" + Rm;
	}

	/**
	 * Gets the assembly instruction, excluding the opcode, for an I-type
	 * instruction
	 *
	 * @param subInstruction substring of the Instruction 10-32 bits
	 * @return instruction
	 */
	public static String getIInstruction(String subInstruction) {
		int immediate = Integer.parseInt(subInstruction.substring(0, 12), 2);
		int Rn = Integer.parseInt(subInstruction.substring(12, 17), 2);
		int Rd = Integer.parseInt(subInstruction.substring(17), 2);

		return " X" + Rd + ", " + "X" + Rn + ", " + "#" + immediate;
	}

	/**
	 * Gets the assembly instruction, excluding the opcode, for a D-type instruction
	 *
	 * @param subInstruction substring of the Instruction 11-32 bits
	 * @return instruction
	 */
	public static String getDInstruction(String subInstruction) {
		int address = Integer.parseInt(subInstruction.substring(0, 9), 2);
		int Rn = Integer.parseInt(subInstruction.substring(11, 16), 2);
		int Rt = Integer.parseInt(subInstruction.substring(16), 2);

		return " X" + Rt + ", " + "[X" + Rn + ", " + "#" + address + "]";
	}

	public static String getBRInstruction(String subInstruction) {
		int address = Integer.parseInt(subInstruction.substring(11, 16), 2);
		return " X" + address;
	}

	public static String getCBInstruction(String subInstruction) {
		int address = binaryToDecimal(subInstruction.substring(8, 27));
		int Rt = Integer.parseInt(subInstruction.substring(27), 2);
		return " X" + Rt + ", " + address;
	}

	public static String getPRNTInstruction(String subInstruction) {
		int Rd = Integer.parseInt(subInstruction.substring(16), 2);
		return " X" + Rd;
	}

	public static Instruction parseInstruction(String input) throws IOException {
		String assembly = "";
		// R and D instructions, 11-bit opcodes
		switch (input.substring(0, 11)) {
		// ADD
		case "10001011000":
			assembly = "ADD" + getRInstruction(input.substring(11), false);
			return new Instruction(assembly, false);
		// AND
		case "10001010000":
			assembly = "AND" + getRInstruction(input.substring(11), false);
			return new Instruction(assembly, false);
		// BR
		case "11010110000":
			assembly = "BR" + getBRInstruction(input.substring(11));
			return new Instruction(assembly, false);
		// EOR
		case "11001010000":
			assembly = "EOR" + getRInstruction(input.substring(11), false);
			return new Instruction(assembly, false);
		// LDUR
		case "11111000010":
			assembly = "LDUR" + getDInstruction(input.substring(11));
			return new Instruction(assembly, false);
		// LSL
		case "11010011011":
			assembly = "LSL" + getRInstruction(input.substring(11), true);
			return new Instruction(assembly, false);
		// LSR
		case "11010011010":
			assembly = "LSR" + getRInstruction(input.substring(11), true);
			return new Instruction(assembly, false);
		// ORR
		case "10101010000":
			assembly = "ORR" + getRInstruction(input.substring(11), false);
			return new Instruction(assembly, false);
		// STUR
		case "11111000000":
			assembly = "STUR" + getDInstruction(input.substring(11));
			return new Instruction(assembly, false);
		// SUB
		case "11001011000":
			assembly = "SUB" + getRInstruction(input.substring(11), false);
			return new Instruction(assembly, false);
		// SUBS
		case "11101011000":
			assembly = "SUBS" + getRInstruction(input.substring(11), false);
			return new Instruction(assembly, false);
		// MUL
		case "10011011000":
			assembly = "MUL" + getRInstruction(input.substring(11), false);
			return new Instruction(assembly, false);
		// PRNT
		case "11111111101":
			assembly = "PRNT" + getPRNTInstruction(input.substring(11));
			return new Instruction(assembly, false);
		// PRNL
		case "11111111100":
			return new Instruction("PRNL", false);
		// DUMP
		case "11111111110":
			return new Instruction("DUMP", false);
		// HALT
		case "11111111111":
			return new Instruction("HALT", false);
		}

		// I type instructions, 10-bit opcodes
		switch (input.substring(0, 10)) {
		// ADDI
		case "1001000100":
			assembly = "ADDI" + getIInstruction(input.substring(10));
			return new Instruction(assembly, false);
		// ANDI
		case "1001001000":
			assembly = "ANDI" + getIInstruction(input.substring(10));
			return new Instruction(assembly, false);
		// EORI
		case "1101001000":
			assembly = "EORI" + getIInstruction(input.substring(10));
			return new Instruction(assembly, false);
		// ORRI
		case "1011001000":
			assembly = "ORRI" + getIInstruction(input.substring(10));
			return new Instruction(assembly, false);
		// SUBI
		case "1101000100":
			assembly = "SUBI" + getIInstruction(input.substring(10));
			return new Instruction(assembly, false);
		// SUBIS
		case "1111000100":
			assembly = "SUBIS" + getIInstruction(input.substring(10));
			return new Instruction(assembly, false);
		}

		// CB type instructions, 8-bit opcode
		switch (input.substring(0, 8)) {
		// CB
		case "01010100":
			// determine & set cond.
			int cond = binaryToDecimal(input.substring(27));
			String condition = "";
			switch (cond) {
			// EQ
			case 0:
				condition += "B.EQ ";
				break;
			// NE
			case 1:
				condition += "B.NE ";
				break;
			// HS
			case 2:
				condition += "B.HS ";
				break;
			// LO
			case 3:
				condition += "B.LO ";
				break;
			// MI
			case 4:
				condition += "B.MI ";
				break;
			// PL
			case 5:
				condition += "B.PL ";
				break;
			// VS
			case 6:
				condition += "B.VS ";
				break;
			// VC
			case 7:
				condition += "B.VC ";
				break;
			// HI
			case 8:
				condition += "B.HI ";
				break;
			// LS
			case 9:
				condition += "B.LS ";
				break;
			// GE
			case 10:
				condition += "B.GE ";
				break;
			// LT
			case 11:
				condition += "B.LT ";
				break;
			// GT
			case 12:
				condition += "B.GT ";
				break;
			// LE
			case 13:
				condition += "B.LE ";
				break;
			}
			condition += binaryToDecimal(input.substring(8, 27));
			return new Instruction(condition, true);
		// CBZ
		case "10110100":
			assembly = "CBZ " + getCBInstruction(input);
			return new Instruction(assembly, true);
		// CBNZ
		case "10110101":
			assembly = "CBNZ " + getCBInstruction(input);
			return new Instruction(assembly, true);
		}

		// B type instructions, 6-bit opcode
		switch (input.substring(0, 6)) {
		// B
		case "000101":
			assembly = "B " + binaryToDecimal(input.substring(6));
			return new Instruction(assembly, true);
		// BL
		case "100101":
			assembly = "BL " + binaryToDecimal(input.substring(6));
			return new Instruction(assembly, true);
		}
		return new Instruction("No Opcode", false);
	}

	static class Instruction {
		protected String instruction;
		protected String label;
		protected boolean branch;
		protected boolean printLabel;

		public Instruction(String instruction, boolean branch) {
			this.instruction = instruction;
			this.label = "";
			this.branch = branch;
			this.printLabel = false;
		}

		public String getInstruction() {
			return instruction;
		}

		public String getLabel()

		{
			return label;
		}

		public boolean isBranch() {
			return branch;
		}

		public boolean isPrintLabel() {
			return printLabel;
		}

		public void setInstruction(String binary) {
			this.instruction = binary;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public void setBranch(boolean branch) {
			this.branch = branch;
		}

		public void setPrintLabel(boolean printLabel) {
			this.printLabel = printLabel;
		}
	}
}