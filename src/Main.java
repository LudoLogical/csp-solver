import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

@SuppressWarnings({"unused", "DuplicatedCode"})
public class Main {

    private static int currentOutputLine;

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

        public boolean isNotSatisfiedBy(int leftValue, int rightValue) {
            switch (this.type) {
                case EQUAL:
                    return leftValue != rightValue;
                case NOT_EQUAL:
                    return leftValue == rightValue;
                case LESS_THAN:
                    return leftValue >= rightValue;
                case GREATER_THAN:
                    return leftValue <= rightValue;
                default:
                    return true; // should never get here
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
        int minNumLegalValuesRemaining = Integer.MAX_VALUE;
        for (String variable : unassignedVariables) {
            ArrayList<Integer> curLegalValuesRemaining = legalValuesRemaining.get(variable);
            if (curLegalValuesRemaining.size() < minNumLegalValuesRemaining) {
                minNumLegalValuesRemaining = curLegalValuesRemaining.size();
                candidateVars.clear();
            }
            if (curLegalValuesRemaining.size() <= minNumLegalValuesRemaining) {
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
            int sum = 0; // want o2Sum - o1Sum
            for (String s : o1.getValue().keySet()) {
                sum -= o1.getValue().get(s).size();
            }
            for (String s : o2.getValue().keySet()) {
                sum += o2.getValue().get(s).size();
            }
            // Smaller key breaks ties
            return sum == 0 ? o1.getKey() - o2.getKey() : sum;
        });

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
            StringBuilder report = new StringBuilder(currentOutputLine + ".\t");
            for (String variable : currentAssignment.keySet()) {
                report.append(variable).append("=").append(currentAssignment.get(variable)).append(", ");
            }
            // remove trailing ", "
            System.out.println(report.substring(0, report.length() - 2) + "\tsolution");
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
                        if (c.rightVariable.name.equals(variableToAssign) && c.isNotSatisfiedBy(valueToCheck, valueToAssign)) {
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

        Set<String> newUnassignedVariables = new HashSet<>(unassignedVariables);
        newUnassignedVariables.remove(variableToAssign);


        int[] domainValueOrder = orderDomainValues(newLegalValuesRemaining);

        for (int valueToAssign : domainValueOrder) {
            boolean isConsistent = true;
            for (Constraint c : variables.get(variableToAssign).constraints) {
                Integer rightValue = currentAssignment.get(c.rightVariable.name);
                if (rightValue != null && c.isNotSatisfiedBy(valueToAssign, rightValue)) {
                    isConsistent = false;
                    break;
                }
            }
            if (isConsistent) {
                @SuppressWarnings("unchecked") LinkedHashMap<String, Integer> newCurrentAssignment
                        = (LinkedHashMap<String, Integer>) currentAssignment.clone();
                newCurrentAssignment.put(variableToAssign, valueToAssign);
                LinkedHashMap<String, Integer> result = solveCSPHelper(newUnassignedVariables,
                        variables, newCurrentAssignment, newLegalValuesRemaining.get(valueToAssign));
                if (result != null) {
                    return result;
                }
            } else {
                StringBuilder report = new StringBuilder(currentOutputLine + ".\t");
                for (String variable : currentAssignment.keySet()) {
                    report.append(variable).append("=").append(currentAssignment.get(variable)).append(", ");
                }
                report.append(variableToAssign).append("=").append(valueToAssign).append("\tfailure");
                System.out.println(report);
                currentOutputLine++;
            }
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

        currentOutputLine = 1;
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
        System.out.println();
        LinkedHashMap<String, Integer> solution = solveCSP(variables);
        System.out.println();
        System.out.println(solution);

    }

}
