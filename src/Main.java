import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

@SuppressWarnings({"unused", "DuplicatedCode"})
public class Main {

    enum ConstraintType {
        EQUAL,
        NOT_EQUAL,
        LESS_THAN,
        GREATER_THAN
    }

    static class Constraint {

        private final ConstraintType type;
        public final Variable rightVariable;

        public Constraint(ConstraintType type, Variable rightVariable) {
            this.type = type;
            this.rightVariable = rightVariable;
        }

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

    private static String selectUnassignedVariable(Set<String> unassignedVariables,
                                                   LinkedHashMap<String, Variable> variables,
                                                   LinkedHashMap<String, ArrayList<Integer>> legalValuesRemaining) {

        ArrayList<String> candidateVars = new ArrayList<>();

        // Heuristic 1: Most Constrained Variable
        int maxNumLegalValuesRemaining = 0;
        for (String variable : unassignedVariables) {
            ArrayList<Integer> curLegalValuesRemaining = legalValuesRemaining.get(variable);
            if (curLegalValuesRemaining.size() > maxNumLegalValuesRemaining) {
                maxNumLegalValuesRemaining = curLegalValuesRemaining.size();
                candidateVars.clear();
            }
            if (curLegalValuesRemaining.size() >= maxNumLegalValuesRemaining) {
                candidateVars.add(variable);
            }
        }

        // Heuristic 2: Most Constraining Variable
        if (candidateVars.size() > 1) {
            ArrayList<String> reducedCandidateVars = new ArrayList<>();
            int maxConstraintsOnRemainingVariables = 0;
            for (String variable : candidateVars) {
                int curConstraintsOnRemainingVariables = 0;
                for (Constraint c : variables.get(variable).constraints) {
                    if (unassignedVariables.contains(c.rightVariable.name)) {
                        curConstraintsOnRemainingVariables++;
                    }
                }
                if (curConstraintsOnRemainingVariables > maxConstraintsOnRemainingVariables) {
                    maxConstraintsOnRemainingVariables = curConstraintsOnRemainingVariables;
                    reducedCandidateVars.clear();
                }
                if (curConstraintsOnRemainingVariables >= maxConstraintsOnRemainingVariables) {
                    reducedCandidateVars.add(variable);
                }
            }
            candidateVars = reducedCandidateVars;
        }

        Collections.sort(candidateVars); // Break further ties alphabetically
        return candidateVars.get(0);

    }

    // Sort in decreasing order of total number of newLegalValuesRemaining, smaller key breaks ties
    private static int[] orderDomainValues(LinkedHashMap<Integer, LinkedHashMap<String, ArrayList<Integer>>> newLegalValuesRemaining) {

        ArrayList<Map.Entry<Integer, LinkedHashMap<String, ArrayList<Integer>>>> sorted
                = new ArrayList<>(newLegalValuesRemaining.entrySet());
        sorted.sort((o1, o2) -> {
            int o1Sum = 0;
            int o2Sum = 0;
            for (String s : o1.getValue().keySet()) {
                o1Sum += o1.getValue().get(s).size();
            }
            for (String s : o2.getValue().keySet()) {
                o1Sum += o2.getValue().get(s).size();
            }
            return o2Sum - o1Sum;
        });

        // Enforce tie-breaking procedure: https://stackoverflow.com/questions/5164902/sorting-a-part-of-java-arraylist

        int[] outputList = new int[sorted.size()];
        int i = 0;
        for (Map.Entry<Integer, LinkedHashMap<String, ArrayList<Integer>>> entry : sorted) {
            outputList[i] = entry.getKey();
            i++;
        }
        return outputList;

    }

    private static LinkedHashMap<String, Integer> solveCSPHelper(Set<String> unassignedVariables,
                                                                 LinkedHashMap<String, Variable> variables,
                                                                 LinkedHashMap<String, Integer> currentAssignment,
                                                                 LinkedHashMap<String, ArrayList<Integer>> legalValuesRemaining) {

        if (variables.size() == currentAssignment.size()) {
            return currentAssignment;
        }

        String variableToAssign = selectUnassignedVariable(unassignedVariables, variables, legalValuesRemaining);

        LinkedHashMap<Integer, LinkedHashMap<String, ArrayList<Integer>>> newLegalValuesRemaining = new LinkedHashMap<>();
        for (int valueToAssign : legalValuesRemaining.get(variableToAssign)) {
            LinkedHashMap<String, ArrayList<Integer>> newLegalValuesRemainingForValueToAssign = new LinkedHashMap<>();
            for (String otherUnassignedVariable : unassignedVariables) {
                if (variableToAssign.equals(otherUnassignedVariable)) {
                    continue;
                }
                ArrayList<Integer> newLegalValuesRemainingForOther = new ArrayList<>();
                for (int valueToCheck : legalValuesRemaining.get(otherUnassignedVariable)) {
                    boolean acceptable = true;
                    for (Constraint c : variables.get(otherUnassignedVariable).constraints) {
                        if (c.rightVariable.name.equals(variableToAssign) && !c.satisfiedBy(valueToCheck, valueToAssign)) {
                            acceptable = false;
                            break;
                        }
                    }
                    if (acceptable) {
                        newLegalValuesRemainingForOther.add(valueToCheck);
                    }
                }
                newLegalValuesRemainingForValueToAssign.put(otherUnassignedVariable, newLegalValuesRemainingForOther);
            }
            newLegalValuesRemaining.put(valueToAssign, newLegalValuesRemainingForValueToAssign);
        }

        int[] domainValueOrder = orderDomainValues(newLegalValuesRemaining);

        for (int v : domainValueOrder) {
            // if v is consistent with currentAssignment
                // make new currentAssignment with v added
                // returnedAssignment = recurse...
                // if returnedAssignment != null then return it
        }

        return null;

    }

    public static LinkedHashMap<String, Integer> solveCSP(LinkedHashMap<String, Variable> variables) {
        LinkedHashMap<String, ArrayList<Integer>> legalValuesRemaining = new LinkedHashMap<>();
        for (String curVar : variables.keySet()) {
            ArrayList<Integer> curLegalValuesRemaining = new ArrayList<>(variables.keySet().size());
            for (int value : variables.get(curVar).domain) {
                curLegalValuesRemaining.add(value);
            }
            legalValuesRemaining.put(curVar, curLegalValuesRemaining);
        }
        return solveCSPHelper(variables.keySet(), variables, new LinkedHashMap<>(), legalValuesRemaining);
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
