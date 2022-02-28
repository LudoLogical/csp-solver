import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Scanner;

@SuppressWarnings({"unused", "DuplicatedCode", "EnhancedSwitchMigration"})
public class Main {

    enum ConstraintType {
        EQUAL,
        NOT_EQUAL,
        LESS_THAN,
        GREATER_THAN
    }

    @SuppressWarnings("EnhancedSwitchMigration")
    record Constraint(ConstraintType type, Variable rightVariable) {
        public boolean satisfiedBy(int leftValue, int rightValue) {
            switch (this.type) {
                case EQUAL:
                    return leftValue == rightValue;
                case NOT_EQUAL:
                    return leftValue != rightValue;
                case LESS_THAN:
                    return leftValue < rightValue;
                case GREATER_THAN:
                    return leftValue > rightValue;
                default:
                    return false; // should never get here
            }
        }
        public String toString() {
            switch (this.type) {
                case EQUAL:
                    return "== " + this.rightVariable.name;
                case NOT_EQUAL:
                    return "!= " + this.rightVariable.name;
                case LESS_THAN:
                    return "< " + this.rightVariable.name;
                case GREATER_THAN:
                    return "> " + this.rightVariable.name;
                default:
                    return "Something went wrong."; // should never get here
            }
        }
    }

    static class Variable {

        // all package-private
        final String name;
        final int[] domain;
        ArrayList<Constraint> constraints = new ArrayList<>();

        public Variable(String name, int[] domain) {
            this.name = name;
            this.domain = domain;
        }

        public void addConstraint(Constraint toAdd) {
            this.constraints.add(toAdd);
        }

        public String toString() {
            StringBuilder out = new StringBuilder("Variable " + this.name + ":\nDomain = {");
            for (int i = 0; i < this.domain.length; i++) {
                out.append(domain[i]);
                if (i < this.domain.length - 1) {
                    out.append(" ");
                }
            }
            out.append("},\nConstraints = {");
            for (int i = 0; i < this.constraints.size(); i++) {
                out.append(this.constraints.get(i).toString());
                if (i < this.constraints.size() - 1) {
                    out.append(", ");
                }
            }
            out.append("}\n");
            return out.toString();
        }

    }

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

        LinkedHashMap<String, Variable> variables = new LinkedHashMap<>();
        while (scan.hasNextLine()) {
            String[] varContent = scan.nextLine().split(":? ");
            int[] domain = new int[varContent.length - 1];
            for (int i = 1; i < varContent.length; i++) {
                domain[i-1] = Integer.parseInt(varContent[i]);
            }
            variables.put(varContent[0], new Variable(varContent[0], domain));
        }
        scan.close();

        try {
            scan = new Scanner(conFile);
        } catch (FileNotFoundException e) {
            System.out.println("Something went wrong opening path_to_con_file.");
            return;
        }

        while (scan.hasNextLine()) {
            String[] conContent = scan.nextLine().split(" ");
            Variable leftVariable = variables.get(conContent[0]);
            Variable rightVariable = variables.get(conContent[2]);
            switch (conContent[1]) {
                case "=":
                    leftVariable.addConstraint(new Constraint(ConstraintType.EQUAL, rightVariable));
                    rightVariable.addConstraint(new Constraint(ConstraintType.EQUAL, leftVariable));
                    break;
                case "!":
                    leftVariable.addConstraint(new Constraint(ConstraintType.NOT_EQUAL, rightVariable));
                    rightVariable.addConstraint(new Constraint(ConstraintType.NOT_EQUAL, leftVariable));
                    break;
                case "<":
                    leftVariable.addConstraint(new Constraint(ConstraintType.LESS_THAN, rightVariable));
                    rightVariable.addConstraint(new Constraint(ConstraintType.GREATER_THAN, leftVariable));
                    break;
                case ">":
                    leftVariable.addConstraint(new Constraint(ConstraintType.GREATER_THAN, rightVariable));
                    rightVariable.addConstraint(new Constraint(ConstraintType.LESS_THAN, leftVariable));
                    break;
                default:
                    System.out.println("Constraints were not correctly specified.");
                    return;
            }
        }
        scan.close();

        System.out.println(variables);

    }

}
