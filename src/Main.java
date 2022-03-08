import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {

    // Used by solveCSP, reset to 1 on each call
    private static int currentOutputLine;

    enum ConstraintType {
        EQUAL,
        NOT_EQUAL,
        LESS_THAN,
        GREATER_THAN
    }

    /**
     * A single constraint on the Variable object to which it belongs. The owner Variable is on the
     * left and the rightVariable on the right of the binary operator indicated by type.
     */
    static class Constraint {

        private final ConstraintType type;
        public final Variable rightVariable;

        public Constraint(ConstraintType type, Variable rightVariable) {
            this.type = type;
            this.rightVariable = rightVariable;
        }

        /**
         * Determines whether the given values satisfy this Constraint.
         * @param leftValue the value on the left of the binary operator associated with this Constraint
         * @param rightValue the value on the right of the binary operator associated with this Constraint
         * @return true if (leftValue [this Constraint] rightValue) is false, false otherwise
         */
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

    /**
     * A single variable from a CSP. Its domain is the collection of all values that it can take on,
     * and the validity of its given value is determined by the specified constraints, if any.
     */
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

    /**
     * Uses the Most Constrained Variable and Most Constraining Variable heuristics, and alphabetic order
     * when breaking ties, to choose a Variable for assignment from the set of unassignedVariables.
     * @param unassignedVariables the Set of Variables from this CSP that do not currently have values assigned to them
     * @param variables a LinkedHashMap joining names of Variables to their corresponding objects
     * @param legalValuesRemaining null (if not using forward checking), or a LinkedHashMap joining names of unassigned
     *                             Variables to ArrayLists of the values that are still legal for them to take on
     *                             (if using forward checking)
     * @return the (single) Variable from the Set of unassignedVariables chosen for assignment.
     */
    private static String selectUnassignedVariable(Set<String> unassignedVariables,
                                                   LinkedHashMap<String, Variable> variables,
                                                   LinkedHashMap<String, ArrayList<Integer>> legalValuesRemaining) {

        ArrayList<String> candidateVars = new ArrayList<>();

        // Heuristic 1: Most Constrained Variable

        if (legalValuesRemaining != null) {

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

        } else {

            ArrayList<String> unassignedVariablesList = new ArrayList<>(unassignedVariables);
            unassignedVariablesList.sort(Comparator.comparingInt(o -> variables.get(o).domain.length));
            int smallestDomainSize = variables.get(unassignedVariablesList.get(0)).domain.length;

            for (String var : unassignedVariablesList) { // proceeds in order
                if (variables.get(var).domain.length == smallestDomainSize) {
                    candidateVars.add(var);
                } else {
                    break;
                }
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

    /**
     * Produces a mapping from remaining legal values in the domain of the variableToAssign to new
     * legalValuesRemaining objects that reflect the decision to assign those values.
     * @param newUnassignedVariables the Set of Variables from this CSP that will still not have
     *                               values assigned to them after variableToAssign is given a value
     * @param variables a LinkedHashMap joining names of Variables to their corresponding objects
     * @param variableToAssign the variable whose values are to be evaluated for assignment
     * @param oldLegalValuesRemaining null (if not using forward checking), or a LinkedHashMap joining names of
     *                                currently unassigned Variables to ArrayLists of the values that are currently
     *                                legal for them to take on (if using forward checking)
     * @return a mapping from remaining legal values in the domain of the variableToAssign to new
     *         legalValuesRemaining objects that reflect the decision to assign those values.
     */
    private static LinkedHashMap<Integer, LinkedHashMap<String, ArrayList<Integer>>> makeFutureMapWithFC(
            Set<String> newUnassignedVariables,
            LinkedHashMap<String, Variable> variables,
            String variableToAssign,
            LinkedHashMap<String, ArrayList<Integer>> oldLegalValuesRemaining
    ) {

        LinkedHashMap<Integer, LinkedHashMap<String, ArrayList<Integer>>> newLegalValuesRemaining = new LinkedHashMap<>();
        for (int assignableValue : oldLegalValuesRemaining.get(variableToAssign)) {
            LinkedHashMap<String, ArrayList<Integer>> newLegalValuesRemainingForAssignableValue = new LinkedHashMap<>();
            for (String otherUnassignedVariable : newUnassignedVariables) {

                ArrayList<Integer> newLegalValuesRemainingForOther = new ArrayList<>();
                for (int valueToCheck : oldLegalValuesRemaining.get(otherUnassignedVariable)) {

                    boolean acceptable = true;
                    for (Constraint c : variables.get(otherUnassignedVariable).constraints) {
                        if (c.rightVariable.name.equals(variableToAssign) && c.isNotSatisfiedBy(valueToCheck, assignableValue)) {
                            acceptable = false;
                            break;
                        }
                    }

                    if (acceptable) {
                        newLegalValuesRemainingForOther.add(valueToCheck);
                    }

                }

                newLegalValuesRemainingForAssignableValue.put(otherUnassignedVariable, newLegalValuesRemainingForOther);
            }
            newLegalValuesRemaining.put(assignableValue, newLegalValuesRemainingForAssignableValue);
        }

        return newLegalValuesRemaining;

    }

    /**
     * Produces a mapping from values in the domain of the variableToAssign to sub-mappings, which are from
     * other unassigned Variable names to ArrayLists of the values that they could still take on if the
     * corresponding variableToAssign domain value were to be chosen.
     * @param newUnassignedVariables the Set of Variables from this CSP that will still not have
     *                               values assigned to them after variableToAssign is given a value
     * @param variables a LinkedHashMap joining names of Variables to their corresponding objects
     * @param variableToAssign the variable whose values are to be evaluated for assignment
     * @return a mapping from values in the domain of the variableToAssign to sub-mappings, which are from
     *         other unassigned Variable names to ArrayLists of the values that they could still take on if the
     *         corresponding variableToAssign domain value were to be chosen.
     */
    private static LinkedHashMap<Integer, LinkedHashMap<String, ArrayList<Integer>>> makeFutureMapWithoutFC(
            Set<String> newUnassignedVariables,
            LinkedHashMap<String, Variable> variables,
            String variableToAssign
    ) {

        LinkedHashMap<Integer, LinkedHashMap<String, ArrayList<Integer>>> acceptableValues = new LinkedHashMap<>();
        for (int assignableValue : variables.get(variableToAssign).domain) {
            LinkedHashMap<String, ArrayList<Integer>> acceptableValuesForAssignableValue = new LinkedHashMap<>();
            for (String otherVariable : newUnassignedVariables) {

                Stream<Integer> boxedStream = IntStream.of(variables.get(otherVariable).domain).boxed();
                ArrayList<Integer> acceptableValuesForOtherVariable = boxedStream.collect(Collectors.toCollection(ArrayList::new));

                for (Constraint c : variables.get(otherVariable).constraints) {
                    if (c.rightVariable.name.equals(variableToAssign)) {
                        for (int valueToCheck : variables.get(otherVariable).domain) {
                            if (c.isNotSatisfiedBy(valueToCheck, assignableValue)) {
                                acceptableValuesForOtherVariable.remove(Integer.valueOf(valueToCheck));
                            }
                        }
                    }
                }

                acceptableValuesForAssignableValue.put(otherVariable, acceptableValuesForOtherVariable);
            }
            acceptableValues.put(assignableValue, acceptableValuesForAssignableValue);
        }

        return acceptableValues;

    }

    /**
     * Uses the Least Constraining Value heuristic, and increasing numerical order when breaking ties, to sort the
     * legal or acceptable values (specifically, the keys in the futureMap) associated with a Variable to be assigned
     * in the order in which they should be tried.
     * @param futureMap a mapping from legal or acceptable values in the domain of a Variable to be assigned to
     *                  sub-mappings, which are from other unassigned Variable names to ArrayLists of the values that
     *                  would be still be legal or acceptable if the corresponding variableToAssign domain value were
     *                  to be chosen.
     * @return a sorted array of values, taken from the keys in the futureMap, to be assigned to some Variable
     */
    private static int[] orderDomainValuesFromFutureMap(LinkedHashMap<Integer, LinkedHashMap<String, ArrayList<Integer>>> futureMap) {

        ArrayList<Map.Entry<Integer, LinkedHashMap<String, ArrayList<Integer>>>> sorted
                = new ArrayList<>(futureMap.entrySet());

        sorted.sort((o1, o2) -> {

            int sum = 0; // want o2Sum - o1Sum
            for (String s : o1.getValue().keySet()) {
                sum -= o1.getValue().get(s).size();
            }
            for (String s : o2.getValue().keySet()) {
                sum += o2.getValue().get(s).size();
            }
            return sum == 0 ? o1.getKey() - o2.getKey() : sum; // Smaller key breaks ties

        });

        int i = 0;
        int[] outputList = new int[sorted.size()];

        for (Map.Entry<Integer, LinkedHashMap<String, ArrayList<Integer>>> entry : sorted) {
            outputList[i] = entry.getKey();
            i++;
        }

        return outputList;

    }

    // Recursive helper function for solveCSP()
    private static LinkedHashMap<String, Integer> solveCSPHelper(Set<String> unassignedVariables,
                                                                 LinkedHashMap<String, Variable> variables,
                                                                 LinkedHashMap<String, Integer> currentAssignment,
                                                                 LinkedHashMap<String, ArrayList<Integer>> legalValuesRemaining) {

        // If currentAssignment is complete, report as much and return it
        if (variables.size() == currentAssignment.size()) {
            StringBuilder report = new StringBuilder(currentOutputLine + ". ");
            for (String variable : currentAssignment.keySet()) {
                report.append(variable).append("=").append(currentAssignment.get(variable)).append(", ");
            }
            // remove trailing ", "
            System.out.println(report.substring(0, report.length() - 2) + "  solution");
            return currentAssignment;
        }

        // Pick the next variable to assign
        String variableToAssign = selectUnassignedVariable(unassignedVariables, variables, legalValuesRemaining);

        // Remove the next variable to assign from unassignedVariables
        Set<String> newUnassignedVariables = new HashSet<>(unassignedVariables);
        newUnassignedVariables.remove(variableToAssign);

        // Decide the order in which to try assigning legal/acceptable values to the variableToAssign
        LinkedHashMap<Integer, LinkedHashMap<String, ArrayList<Integer>>> futureMap;
        if (legalValuesRemaining != null) {
            futureMap = makeFutureMapWithFC(newUnassignedVariables, variables, variableToAssign, legalValuesRemaining);
        } else {
            futureMap = makeFutureMapWithoutFC(newUnassignedVariables, variables, variableToAssign);
        }
        int[] domainValueOrder = orderDomainValuesFromFutureMap(futureMap);

        // For every legal/acceptable value, check that it is consistent with all of variableToAssign's Constraints
        for (int valueToAssign : domainValueOrder) {

            boolean isConsistent = true;
            for (Constraint c : variables.get(variableToAssign).constraints) {

                // If the other Variable associated w/ this Constraint
                // has already been assigned, check for a contradiction
                if (currentAssignment.containsKey(c.rightVariable.name)) {

                    if (c.isNotSatisfiedBy(valueToAssign, currentAssignment.get(c.rightVariable.name))) {
                        isConsistent = false;
                        break;
                    }

                // Otherwise, only if using forward checking, make sure that there is still at least one legal value
                // for the other Variable associated w/ this Constraint to take on after the assignment under
                // consideration goes through
                } else if (legalValuesRemaining != null) {

                    isConsistent = false;
                    for (int v : c.rightVariable.domain) {
                        if (!c.isNotSatisfiedBy(valueToAssign, v)) {
                            isConsistent = true;
                            break;
                        }
                    }
                    if (!isConsistent) {
                        break;
                    }

                }

            }

            // If there are no objections to this assignment going through, make the assignment
            if (isConsistent) {

                @SuppressWarnings("unchecked") LinkedHashMap<String, Integer> newCurrentAssignment
                        = (LinkedHashMap<String, Integer>) currentAssignment.clone();
                newCurrentAssignment.put(variableToAssign, valueToAssign);

                // Check if this assignment eventually results in success
                LinkedHashMap<String, Integer> result;
                if (legalValuesRemaining != null) {
                    result = solveCSPHelper(newUnassignedVariables, variables,
                            newCurrentAssignment, futureMap.get(valueToAssign));
                } else {
                    result = solveCSPHelper(newUnassignedVariables, variables,
                            newCurrentAssignment, null);
                }

                // If success, unwind all the recursion; otherwise, try something else
                if (result != null) {
                    return result;
                }

            // If there are objections to this assignment going through, report and return failure
            } else {

                StringBuilder report = new StringBuilder(currentOutputLine + ". ");
                for (String variable : currentAssignment.keySet()) {
                    report.append(variable).append("=").append(currentAssignment.get(variable)).append(", ");
                }
                report.append(variableToAssign).append("=").append(valueToAssign).append("  failure");
                System.out.println(report);
                currentOutputLine++;

            }

        }

        return null;

    }

    /**
     * Solves the CSP with the given Variables and the Constraints they own, if possible.
     * @param variables a LinkedHashMap joining names of Variables to their corresponding objects
     * @param useForwardChecking true if the solver should use forward checking, false otherwise
     * @return a mapping from input Variable names to values within their domains that satisfy all
     *         Constraints owned by all Variables; null otherwise
     */
    public static LinkedHashMap<String, Integer> solveCSP(LinkedHashMap<String, Variable> variables,
                                                          boolean useForwardChecking) {

        LinkedHashMap<String, ArrayList<Integer>> legalValuesRemaining = null;

        if (useForwardChecking) {

            legalValuesRemaining = new LinkedHashMap<>();
            for (String curVar : variables.keySet()) {
                ArrayList<Integer> curLegalValuesRemaining = new ArrayList<>(variables.keySet().size());
                for (int value : variables.get(curVar).domain) {
                    curLegalValuesRemaining.add(value);
                }
                legalValuesRemaining.put(curVar, curLegalValuesRemaining);
            }

        }

        currentOutputLine = 1;
        return solveCSPHelper(variables.keySet(), variables, new LinkedHashMap<>(), legalValuesRemaining);

    }

    public static void main(String[] args) {

        // Check for the correct number of args
        if (args.length != 3) {
            System.out.println("Usage: java Main <path_to_var_file> <path_to_con_file> <none|fc>");
            return;
        }

        // Create file objects for relevant paths
        File varFile = new File(args[0]);
        File conFile = new File(args[1]);
        Scanner scan;

        // Try to open varFile
        try {
            scan = new Scanner(varFile);
        } catch (FileNotFoundException e) {
            System.out.println("Something went wrong opening path_to_var_file.");
            return;
        }

        // Set up Variable objects
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

        // Try ot open conFile
        try {
            scan = new Scanner(conFile);
        } catch (FileNotFoundException e) {
            System.out.println("Something went wrong opening path_to_con_file.");
            return;
        }

        // Set up Constraints for the Variable objects
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

        // solution is not needed since every outcome is reported by solveCSP()
        @SuppressWarnings("unused") LinkedHashMap<String, Integer> solution = solveCSP(variables, args[2].equals("fc"));

    }

}
