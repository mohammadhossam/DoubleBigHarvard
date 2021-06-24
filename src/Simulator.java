import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

public class Simulator {

    static short[] instructionMemory;
    static byte[] dataMemory, registerFile;
    static short pc;
    static byte statusReg;
    static int clockCycle, numOfInstructions, changedReg, changedMem;

    static short fetched, toBeDecoded, toBeExecuted;
    static boolean decodeFlag, executeFlag, registerUpdated, memoryUpdated;

    static HashMap<String, Byte> decodedInstruction;
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

    }

    public static void decode() {

    }

    public static void execute() {

        byte opCode = decodedInstruction.get("OPCODE");
        byte srcRegister1 = decodedInstruction.get("R1");

        if (decodedInstruction.containsKey("R2")) {
            //I have an R-Instruction
            byte srcRegister2 = decodedInstruction.get("R2");
            registerUpdated = true;
            changedReg = srcRegister1;
            //In jump instruction set the flag to false
            int result = 0;
            switch (opCode) {
                case 0: //ADD
                    result = (registerFile[srcRegister1] + registerFile[srcRegister2]);

                    if (overFlowOccured(registerFile[srcRegister1],registerFile[srcRegister2],result))
                        statusReg |= 1 << 3; //for Overflow flag

                    registerFile[srcRegister1] = (byte) result;
                    break;

                case 1: //SUB
                    result = (registerFile[srcRegister1] - registerFile[srcRegister2]);
                    if (overFlowOccured(registerFile[srcRegister1],registerFile[srcRegister2],result))
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
                    registerFile[srcRegister1]=(byte)(registerFile[srcRegister1]|registerFile[srcRegister2]);
                    break;
                case 7://concatenation
                    result=(byte) ((registerFile[srcRegister1]<<4)|registerFile[srcRegister2]);
                    if (overFlowOccured(registerFile[srcRegister1],registerFile[srcRegister2],result))
                        statusReg |= 1 << 3; //for Overflow flag
                    registerFile[srcRegister1]= (byte) result;
                    break;
            }
            if (result > Byte.MAX_VALUE) //for carry flag
                statusReg |= 1 << 4;
            if (registerFile[srcRegister1] < 0)
                statusReg |= 1 << 2; //for Negative flag
            if (registerFile[srcRegister1] == 0)
                statusReg |= 1; //for Zero flag
            if (opCode == 0 || opCode == 1)  //only in ADD and SUB
                statusReg |= ((statusReg >> 2 & 1) ^ (statusReg >> 3 & 1)) << 1; //for Sign flag
        } else {
            //I have an I-Instruction
            byte IMM = decodedInstruction.get("IMMEDIATE");
            switch (opCode) {
                case 3: //LDI
                    registerFile[srcRegister1] = IMM;
                    registerUpdated = true;
                    changedReg = srcRegister1;
                    break;
                case 4: //BEQZ
                    pc = srcRegister1 == 0 ? pc += IMM : pc;
                    break;
                case 8://SLC
                    registerFile[srcRegister1]=(byte) (registerFile[srcRegister1]<<IMM|registerFile[srcRegister1]>>>(8-IMM));
                    break;
                case 9://SRC
                    registerFile[srcRegister1]=(byte) (registerFile[srcRegister1]>>>IMM|registerFile[srcRegister1]<<(8-IMM));
                    break;
                case 10://LB
                    registerFile[srcRegister1]=dataMemory[IMM];
                    break;
                case 11://SB
                    dataMemory[IMM]=registerFile[srcRegister1];
                    break;
            }

        }

    }

    private static boolean overFlowOccured(byte R1,byte R2, int result) {
        return (R1> 0 && R2> 0 & ((byte) result) < 0)
                || (R1< 0 && R2 < 0 & ((byte) result) > 0);
    }

    public static void main(String[] args) {
        String filePath = "";
        Simulator simulator = new Simulator(filePath);

        clockCycle = 1;
        while ((numOfInstructions--) + 2 > 0) {
            System.out.println("Current Clock Cycle: " + clockCycle + ".");
            System.out.println();
            fetch();
            System.out.println("The instruction being fetched: " + instructionMapping.get(fetched) + ".");
            System.out.println();
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
                System.out.println("Inputs for execute stage:\n\t Opcode: "
                        + decodedInstruction.get("OPCODE") + "\n\t R1: "
                        + decodedInstruction.get("R1") + "\n\t "
                        + (decodedInstruction.containsKey("R2") ?
                        "R2: " + decodedInstruction.get("R2")
                        : "Immediate: " + decodedInstruction.get("IMMEDIATE")));
            } else {
                System.out.println("None.");
            }
            System.out.println();
            if (registerUpdated) {
                System.out.println("Register R" + (changedReg) + " has been changed. New value: "
                        + registerFile[changedReg] + ".");
            }
            System.out.println();
            if (memoryUpdated) {
                System.out.println("Memory[" + (changedMem) + "] has been changed. New value: "
                        + dataMemory[changedMem] + ".");
            }
            clockCycle++;
            System.out.println("-----------------------------------------------------------------------");
        }

        System.out.println("Contents of register file: ");
        for (int i = 0; i < registerFile.length; i++) {
            System.out.println("R" + (i + 1) + ": " + registerFile[i]);
        }
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
