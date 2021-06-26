import java.util.HashMap;

public class Simulator {

    static short[] instructionMemory;
    static byte[] dataMemory, registerFile;
    static short pc, pcOfExecuted;
    static byte statusReg;
    static int numOfInstructions, changedReg, changedMem;

    static short fetched, toBeDecoded, toBeExecuted;
    static boolean registerUpdated, memoryUpdated, pcChanged;

    static HashMap<String, Byte> decodedInstruction, toBeExecutedHashMap, decodedOperands, toBeExecutedOperands;
    static HashMap<Short, String> instructionMapping;

    public Simulator(String filePath) {
        instructionMemory = new short[1024];
        dataMemory = new byte[2048];
        registerFile = new byte[64];
        decodedInstruction = new HashMap<>();
        instructionMapping = new HashMap<>();
        decodedOperands = new HashMap<>();
        toBeExecutedOperands = new HashMap<>();
        // reading text file
        // instantiate Parser with text file (lines[], instructionMemory)
        try {
            Parser p = new Parser(filePath, instructionMemory, instructionMapping);
            numOfInstructions = p.getLinesCount();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void fetch() {
        fetched = instructionMemory[pc];
        pc++;
    }

    public static void decode() {
        byte opcode = (byte) ((toBeDecoded & 0b1111000000000000) >> 12);
        byte r1 = (byte) ((toBeDecoded & 0b0000111111000000) >> 6);
        byte r2 = (byte) (toBeDecoded & 0b0000000000111111);
        byte immediate = (byte) (toBeDecoded & 0b0000000000111111);
        decodedInstruction.clear();
        decodedInstruction.put("OPCODE", opcode);
        decodedInstruction.put("R1", registerFile[r1]);

        decodedOperands.clear();
        decodedOperands.put("R1", r1);

        if (opcode == 0 || opcode == 1 || opcode == 2 || opcode == 5 || opcode == 6 || opcode == 7) {
            decodedInstruction.put("R2", registerFile[r2]);
            decodedOperands.put("R2", r2);
        }
        else {
            if (opcode != 10 && opcode != 11){
                immediate |= (((1 << 5) & immediate) != 0) ? (0b11000000) : 0;
            }
            decodedInstruction.put("IMMEDIATE", immediate);
        }
    }

    public static void execute() {

        byte opCode = toBeExecutedHashMap.get("OPCODE");
        byte src1 = toBeExecutedHashMap.get("R1");
        byte r1 = toBeExecutedOperands.get("R1");

        if (toBeExecutedHashMap.containsKey("R2")) {
            //I have an R-Instruction
            byte src2 = toBeExecutedHashMap.get("R2");
            registerUpdated = true;
            changedReg = r1;
            //In jump instruction set the flag to false
            int result = 0;
            switch (opCode) {
                case 0: //ADD
                    result = (src1 + src2);
                    if (overFlowOccurred(src1, src2, result))
                        statusReg |= 1 << 3; //for Overflow flag
                    if(carryOccurred(src1, src2, (byte) result))
                        statusReg |= 1 << 4;
                    registerFile[r1] = (byte) result;
                    break;

                case 1: //SUB
                    result = (src1 - src2);
                    if (overFlowOccurred(src1, src2, result))
                        statusReg |= 1 << 3; //for Overflow flag
                    if(carryOccurred(src1, src2, (byte) result))
                        statusReg |= 1 << 4;
                    registerFile[r1] = (byte) result;
                    break;

                case 2: //MUL
                    result = (src1 * src2);
                    if(carryOccurred(src1, src2, (byte) result))
                        statusReg |= 1 << 4;
                    registerFile[r1] = (byte) result;
                    break;
                case 5: //AND
                    registerFile[r1] = (byte) (src1 & src2);
                    break;
                case 6://OR
                    registerFile[r1] = (byte) (src1 | src2);
                    break;
                case 7: //JR
                    result = ((src1 << 8) | src2);
                    pc = (short) result;
                    pcChanged = true;
                    registerUpdated = false;
                    break;
            }
            if (registerFile[r1] < 0 && opCode != 7)
                statusReg |= 1 << 2; //for Negative flag
            if (registerFile[r1] == 0 && opCode != 7)
                statusReg |= 1; //for Zero flag
            if (opCode == 0 || opCode == 1)  //only in ADD and SUB
                statusReg |= ((statusReg >> 2 & 1) ^ (statusReg >> 3 & 1)) << 1; //for Sign flag
        } else {
            //I have an I-Instruction
            byte IMM = toBeExecutedHashMap.get("IMMEDIATE");
            switch (opCode) {
                case 3: //LDI
                    registerFile[r1] = IMM;
                    registerUpdated = true;
                    changedReg = r1;
                    break;
                case 4: //BEQZ
                    pc = (short) (src1 == 0 ? pcOfExecuted + IMM + 1 : pc);
                    pcOfExecuted = src1 == 0 ? (short) (pc - 1) : pcOfExecuted;
                    pcChanged = src1 == 0;
                    break;
                case 8://SLC
                    registerFile[r1] = (byte) (src1 << IMM | src1 >>> (8 - IMM));
                    if (registerFile[r1] < 0)
                        statusReg |= 1 << 2; //for Negative flag
                    if (registerFile[r1] == 0)
                        statusReg |= 1; //for Zero flag
                    registerUpdated = true;
                    changedReg = r1;
                    break;
                case 9://SRC
                    registerFile[r1] = (byte) (src1 >>> IMM | src1 << (8 - IMM));
                    if (registerFile[r1] < 0)
                        statusReg |= 1 << 2; //for Negative flag
                    if (registerFile[r1] == 0)
                        statusReg |= 1; //for Zero flag
                    registerUpdated = true;
                    changedReg = r1;
                    break;
                case 10://LB
                    registerFile[r1] = dataMemory[IMM];
                    registerUpdated = true;
                    changedReg = r1;
                    break;
                case 11://SB
                    dataMemory[IMM] = src1;
                    memoryUpdated = true;
                    changedMem = IMM;
                    break;
            }
        }
    }

    private static boolean overFlowOccurred(byte R1, byte R2, int result) {
        return (R1 > 0 && R2 > 0 & ((byte) result) < 0)
                || (R1 < 0 && R2 < 0 & ((byte) result) > 0);
    }

    private static boolean carryOccurred(byte R1, byte R2, byte Result){
        boolean beforeLastCarry = ((1 << 7 & R1) ^ (1 << 7 & R2)) != (1 << 7 & Result);
        if(beforeLastCarry){
            return (1 << 7 & R1) != 0 || (1 << 7 & R2) != 0;
        }
        return (1 << 7 & R1) != 0 && (1 << 7 & R2) != 0;
    }

    public static void main(String[] args) {
        String filePath = "ins";
        Simulator simulator = new Simulator(filePath);
        boolean fetchFlag = true, decodeFlag = false, executeFlag = false;
        int clockCycle = 1;
        int loopLimit = numOfInstructions + 2;
        for (int i = 0; i < loopLimit; i++) {
            if (i > 0) {
                decodeFlag = true;
                if (i > 1) {
                    executeFlag = true;
                }
            }
            if (i >= loopLimit - 2) {
                fetchFlag = false;
                if (i >= loopLimit - 1) {
                    decodeFlag = false;
                }
            }
            System.out.println("Current Clock Cycle: " + clockCycle + ".");
            System.out.println();

            System.out.print("The instruction being fetched: ");
            if (fetchFlag) {
                fetch();
                System.out.println(instructionMapping.get(fetched) + ".");
            } else {
                System.out.println("None.");
            }
            System.out.println();

            toBeExecutedHashMap = new HashMap<>(decodedInstruction);
            toBeExecutedOperands = new HashMap<>(decodedOperands);

            System.out.print("The instruction being decoded: ");
            if (decodeFlag) {
                decode();
                System.out.println(instructionMapping.get(toBeDecoded) + ".");
                System.out.println("Inputs for decode stage:\n\t Instruction in binary: " + Parser.getBinary(toBeDecoded));
            } else {
                System.out.println("None.");
            }
            System.out.println();

            System.out.print("The instruction being executed: ");
            if (executeFlag) {
                execute();
                System.out.println(instructionMapping.get(toBeExecuted) + ".");
                System.out.println("Inputs for execute stage:\n\t OPCODE: "
                        + toBeExecutedHashMap.get("OPCODE") + "\n\t Value of R1: "
                        + toBeExecutedHashMap.get("R1") + "\n\t R1: R"
                        + toBeExecutedOperands.get("R1") + "\n\t "
                        + (toBeExecutedHashMap.containsKey("R2") ?
                        "Value of R2: " + toBeExecutedHashMap.get("R2") + "\n\t R2: R"
                        + toBeExecutedOperands.get("R2")
                        : "IMM: " + toBeExecutedHashMap.get("IMMEDIATE")));
                pcOfExecuted++;
            } else {
                System.out.println("None.");
            }
            System.out.println();
            if(pcChanged) {
                i = -1;
                loopLimit = (numOfInstructions - pc ) + 2;
                fetchFlag = true;
                decodeFlag = false;
                executeFlag = false;
                pcChanged = false;
            }
            toBeExecuted = toBeDecoded;
            toBeDecoded = fetched;

            if (registerUpdated) {
                System.out.println("Register R" + (changedReg) + " has been changed. New value: "
                        + registerFile[changedReg] + ".");
                registerUpdated = false;
            }
            System.out.println();
            if (memoryUpdated) {
                System.out.println("Memory[" + (changedMem) + "] has been changed. New value: "
                        + dataMemory[changedMem] + ".");
                memoryUpdated = false;
            }
            clockCycle++;
            System.out.println("-----------------------------------------------------------------------");
        }

        System.out.println("Contents of register file: ");
        for (int i = 0; i < registerFile.length; i++) {
            System.out.println("R" + (i) + ": " + registerFile[i]);
        }
        System.out.println("PC: " + pc);
        System.out.println("SREG (in binary): " + Parser.getBinary(statusReg).substring(8));
        System.out.println();

        System.out.println("Contents of instruction memory: ");
        for (int i = 0; i < instructionMemory.length; i++) {
            System.out.println("InstructionMemory[" + i + "]: " + instructionMemory[i]);
        }
        System.out.println();

        System.out.println("Contents of data memory: ");
        for (int i = 0; i < dataMemory.length; i++) {
            System.out.println("DataMemory[" + i + "]: " + dataMemory[i]);
        }
    }

}
