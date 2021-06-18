import java.util.HashMap;

public class Simulator {

    static short[] instructionMemory;
    static byte[] dataMemory, registerFile;
    static short pc;
    static byte statusReg;
    static int clockCycle;

    static short fetched, toBeDecoded, toBeExecuted;
    static boolean decodeFlag, executeFlag;

    static HashMap<String, Byte> decodedInstruction;

    public Simulator(String filePath){
        instructionMemory = new short[1024];
        dataMemory = new byte[2048];
        registerFile = new byte[64];
        decodedInstruction = new HashMap<>();

        // reading text file
        // instantiate Parser with text file (lines[], instructionMemory)
    }

    public static void fetch(){

    }

    public static void decode(){

    }

    public static void execute(){

    }

    public static void main(String[] args) {
        // new Simulator
        // handling printings and clock cycle increment
    }

}
