import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

public class Simulator {

    static short[] instructionMemory;
    static byte[] dataMemory, registerFile;
    static short pc;
    static byte statusReg;
    static int numOfInstructions, changedReg, changedMem;

    static short fetched, toBeDecoded, toBeExecuted;
    static boolean registerUpdated, memoryUpdated, pcChanged;

    static HashMap<String, Byte> decodedInstruction, toBeExecutedHashMap;
    static HashMap<Short, String> instructionMapping;

    public Simulator(String filePath) {
        instructionMemory = new short[1024];
        dataMemory = new byte[2048];
        registerFile = new byte[64];
        decodedInstruction = new HashMap<>();
        instructionMapping = new HashMap<>();
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
        decodedInstruction.put("R1", r1);
        if (opcode == 0 || opcode == 1 || opcode == 2 || opcode == 5 || opcode == 6 || opcode == 7)
            decodedInstruction.put("R2", r2);
        else
            decodedInstruction.put("IMMEDIATE", immediate);
    }

    public static void execute() {

        byte opCode = toBeExecutedHashMap.get("OPCODE");
        byte srcRegister1 = toBeExecutedHashMap.get("R1");

        if (toBeExecutedHashMap.containsKey("R2")) {
            //I have an R-Instruction
            byte srcRegister2 = toBeExecutedHashMap.get("R2");
            registerUpdated = true;
            changedReg = srcRegister1;
            //In jump instruction set the flag to false
            int result = 0;
            switch (opCode) {
                case 0: //ADD
                    result = (registerFile[srcRegister1] + registerFile[srcRegister2]);

                    if (overFlowOccured(registerFile[srcRegister1], registerFile[srcRegister2], result))
                        statusReg |= 1 << 3; //for Overflow flag

                    registerFile[srcRegister1] = (byte) result;
                    break;

                case 1: //SUB
                    result = (registerFile[srcRegister1] - registerFile[srcRegister2]);
                    if (overFlowOccured(registerFile[srcRegister1], registerFile[srcRegister2], result))
                        statusReg |= 1 << 3; //for Overflow flag

                    registerFile[srcRegister1] = (byte) result;
                    break;

                case 2: //MUL
                    result = (registerFile[srcRegister1] * registerFile[srcRegister2]);
                    registerFile[srcRegister1] = (byte) result;
                    break;
                case 5: //AND
                    registerFile[srcRegister1] = (byte) (registerFile[srcRegister1] & registerFile[srcRegister2]);
                    break;
                case 6://OR
                    registerFile[srcRegister1] = (byte) (registerFile[srcRegister1] | registerFile[srcRegister2]);
                    break;
                case 7: //JR
                    result = ((registerFile[srcRegister1] << 8) | registerFile[srcRegister2]);
                    if (overFlowOccured(registerFile[srcRegister1], registerFile[srcRegister2], result))
                        statusReg |= 1 << 3; //for Overflow flag
                    pc = (short) result;
                    pcChanged = true;
                    registerUpdated = false;
                    break;
            }
            if (result > Byte.MAX_VALUE && opCode != 7) //for carry flag
                statusReg |= 1 << 4;
            if (registerFile[srcRegister1] < 0 && opCode != 7)
                statusReg |= 1 << 2; //for Negative flag
            if (registerFile[srcRegister1] == 0 && opCode != 7)
                statusReg |= 1; //for Zero flag
            if (opCode == 0 || opCode == 1)  //only in ADD and SUB
                statusReg |= ((statusReg >> 2 & 1) ^ (statusReg >> 3 & 1)) << 1; //for Sign flag
        } else {
            //I have an I-Instruction
            byte IMM = toBeExecutedHashMap.get("IMMEDIATE");
            switch (opCode) {
                case 3: //LDI
                    registerFile[srcRegister1] = IMM;
                    registerUpdated = true;
                    changedReg = srcRegister1;
                    break;
                case 4: //BEQZ
                    pc -= registerFile[srcRegister1] == 0 ? 3 : 0;
                    pc += registerFile[srcRegister1] == 0 ? IMM + 1 : 0;
                    pcChanged = registerFile[srcRegister1] == 0;
                    break;
                case 8://SLC
                    registerFile[srcRegister1] = (byte) (registerFile[srcRegister1] << IMM | registerFile[srcRegister1] >>> (8 - IMM));
                    break;
                case 9://SRC
                    registerFile[srcRegister1] = (byte) (registerFile[srcRegister1] >>> IMM | registerFile[srcRegister1] << (8 - IMM));
                    break;
                case 10://LB
                    registerFile[srcRegister1] = dataMemory[IMM];
                    break;
                case 11://SB
                    dataMemory[IMM] = registerFile[srcRegister1];
                    break;
            }

        }

    }

    private static boolean overFlowOccured(byte R1, byte R2, int result) {
        return (R1 > 0 && R2 > 0 & ((byte) result) < 0)
                || (R1 < 0 && R2 < 0 & ((byte) result) > 0);
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

            System.out.print("The instruction being decoded: ");
            if (decodeFlag) {
                decode();
                System.out.println(instructionMapping.get(toBeDecoded) + ".");
                System.out.println("Inputs for decode stage:\n\t Instruction in decimal: " + toBeDecoded);
            } else {
                System.out.println("None.");
            }
            System.out.println();

            System.out.print("The instruction being executed: ");
            if (executeFlag) {
                execute();
                System.out.println(instructionMapping.get(toBeExecuted) + ".");
                System.out.println("Inputs for execute stage:\n\t OPCODE: "
                        + toBeExecutedHashMap.get("OPCODE") + "\n\t R1: "
                        + toBeExecutedHashMap.get("R1") + "\n\t "
                        + (toBeExecutedHashMap.containsKey("R2") ?
                        "R2: " + toBeExecutedHashMap.get("R2")
                        : "IMM: " + toBeExecutedHashMap.get("IMMEDIATE")));
            } else {
                System.out.println("None.");
            }
            System.out.println();
            if(pcChanged) {
                i = -1;
                loopLimit = (numOfInstructions - pc ) + 2; //BEQZ is not stable
                decodeFlag = false;
                executeFlag = false;
                pcChanged = false;
                System.out.println("PC = " + pc);
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
            System.out.println("R" + (i + 1) + ": " + registerFile[i]);
        }
        System.out.println("PC: " + pc);
        System.out.println("SREG: " + statusReg);
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
