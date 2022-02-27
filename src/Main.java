import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Scanner;

@SuppressWarnings({"unused", "DuplicatedCode", "UnusedAssignment", "CommentedOutCode"})
public class Main {

    public static void main(String[] args) {

        if (args.length != 3) {
            System.out.println("Usage: java Main <path_to_var_file> <path_to_con_file> <none|fc>");
            return;
        }

        File varFile = new File(args[0]);
        File conFile = new File(args[1]);
        Scanner scan;

        try {
            scan = new Scanner(varFile);
        } catch (FileNotFoundException e) {
            System.out.println("Something went wrong opening path_to_var_file.");
            return;
        }

        LinkedHashMap<String, ArrayList<String>> variables = new LinkedHashMap<>();
        while (scan.hasNextLine()) {
            String[] varContent = scan.nextLine().split(" ");
            variables.put(varContent[0], new ArrayList<>(Arrays.asList(varContent).subList(1, varContent.length)));
        }
        scan.close();

        try {
            scan = new Scanner(conFile);
        } catch (FileNotFoundException e) {
            System.out.println("Something went wrong opening path_to_con_file.");
            return;
        }

        // Format: [A][B] = C means that a constraint statement of the form A C B exists
//        ConstraintType[][] constraints = new ConstraintType[variables.size()][variables.size()];
//        while (scan.hasNextLine()) {
//            String[] varContent = scan.nextLine().split(" ");
//
//        }
//        scan.close();

        System.out.println(variables);

    }

}
