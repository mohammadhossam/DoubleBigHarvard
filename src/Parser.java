import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class Parser {

    int linesCount;

    public Parser(String filePath, short[] instructionMemory, HashMap<Short, String> instructionMapping) throws Exception {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));

        String curLine;


        while ((curLine = bufferedReader.readLine()) != null) {
            String[] tokens = curLine.split(" ");
            String opString = tokens[0];

            short instruction;

            short opCode;
            short arg1 = Short.parseShort(tokens[1].substring(1));
            short arg2;

            switch (opString) {
                case "ADD":
                    opCode = 0;
                    arg2 = Short.parseShort(tokens[2].substring(1));
                    break;
                case "SUB":
                    opCode = 1;
                    arg2 = Short.parseShort(tokens[2].substring(1));
                    break;
                case "MUL":
                    opCode = 2;
                    arg2 = Short.parseShort(tokens[2].substring(1));
                    break;
                case "LDI":
                    opCode = 3;
                    arg2 = Short.parseShort(tokens[2]);
                    break;
                case "BEQZ":
                    opCode = 4;
                    arg2 = Short.parseShort(tokens[2]);
                    break;
                case "AND":
                    opCode = 5;
                    arg2 = Short.parseShort(tokens[2].substring(1));
                    break;
                case "OR":
                    opCode = 6;
                    arg2 = Short.parseShort(tokens[2].substring(1));
                    break;
                case "JR":
                    opCode = 7;
                    arg2 = Short.parseShort(tokens[2].substring(1));
                    break;
                case "SLC":
                    opCode = 8;
                    arg2 = Short.parseShort(tokens[2]);
                    break;
                case "SRC":
                    opCode = 9;
                    arg2 = Short.parseShort(tokens[2]);
                    break;
                case "LB":
                    opCode = 10;
                    arg2 = Short.parseShort(tokens[2]);
                    break;
                case "SB":
                    opCode = 11;
                    arg2 = Short.parseShort(tokens[2]);
                    break;
                default:
                    throw new Exception("Not Supported Instruction");
            }

            instruction = setInstruction(opCode, arg1, arg2);
            instructionMemory[linesCount++] = instruction;
            instructionMapping.put(instruction, tokens[0]);

            System.out.println(getBinary(instruction));
        }

    }

    public int getLinesCount() {
        return linesCount;
    }

    private String getBinary(short s) {

        String ans = "";
        for (short i = 0; i < 16; i++) {
            ans = ((s&(1<<i)) == 0?"0":"1") + ans;
            if(i == 5 || i == 11)
                ans = " "+ans;
        }
        return ans;
    }

    private short setInstruction(short opCode, short arg1, short arg2) {
        short instruction = 0;
        instruction |= (opCode << 12);
        instruction |= (arg1 << 6);
        instruction |= arg2;

        return instruction;
    }


    public static void main(String[] args) throws Exception {
        Parser p = new Parser("ins",new short[5],new HashMap<>());

    }
}
