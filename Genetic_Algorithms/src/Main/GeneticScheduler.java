package Main;

import java.util.*;

public class GeneticScheduler {
	
	
	// Defines the actvites that need to be scheduled 
    static String[] activities = {
        "SLA100A", "SLA100B", "SLA191A", "SLA191B", "SLA201",
        "SLA291", "SLA303", "SLA304", "SLA394", "SLA449", "SLA451"
    };
    // Defines the rooms and their capacities 
    static Map<String, Integer> rooms = Map.of(
        "Slater 003", 45, "Roman 216", 30, "Loft 206", 75,
        "Roman 201", 50, "Loft 310", 108, "Beach 201", 60,
        "Beach 301", 75, "Logos 325", 450, "Frank 119", 60
    );
    // Define available timeslots
    static String[] timeslots = {"10 AM", "11 AM", "12 PM", "1 PM", "2 PM", "3 PM"};
    // Define facilitators for the activities
    static String[] facilitators = {"Lock", "Glen", "Banks", "Richards", "Shaw", "Singer", "Uther", "Tyler", "Numen", "Zeldin"};
    // Define preferred facilitators for each activity
    static Map<String, List<String>> preferredFacilitators = new HashMap<>();
    static {
        preferredFacilitators.put("SLA100A", Arrays.asList("Glen", "Lock", "Banks", "Zeldin"));
        preferredFacilitators.put("SLA100B", Arrays.asList("Glen", "Lock", "Banks", "Zeldin"));
        preferredFacilitators.put("SLA191A", Arrays.asList("Glen", "Lock", "Banks", "Zeldin"));
        preferredFacilitators.put("SLA191B", Arrays.asList("Glen", "Lock", "Banks", "Zeldin"));
        preferredFacilitators.put("SLA201", Arrays.asList("Glen", "Banks", "Zeldin", "Shaw"));
        preferredFacilitators.put("SLA291", Arrays.asList("Lock", "Banks", "Zeldin", "Singer"));
        preferredFacilitators.put("SLA303", Arrays.asList("Glen", "Zeldin", "Banks"));
        preferredFacilitators.put("SLA304", Arrays.asList("Glen", "Banks", "Tyler"));
        preferredFacilitators.put("SLA394", Arrays.asList("Tyler", "Singer"));
        preferredFacilitators.put("SLA449", Arrays.asList("Tyler", "Singer", "Shaw"));
        preferredFacilitators.put("SLA451", Arrays.asList("Tyler", "Singer", "Shaw"));
    }
    
    // Define expected enrollment for each activity
    static Map<String, Integer> expectedEnrollment = new HashMap<>();
    static {
        expectedEnrollment.put("SLA100A", 50);
        expectedEnrollment.put("SLA100B", 50);
        expectedEnrollment.put("SLA191A", 50);
        expectedEnrollment.put("SLA191B", 50);
        expectedEnrollment.put("SLA201", 50);
        expectedEnrollment.put("SLA291", 50);
        expectedEnrollment.put("SLA303", 60);
        expectedEnrollment.put("SLA304", 25);
        expectedEnrollment.put("SLA394", 20);
        expectedEnrollment.put("SLA449", 60);
        expectedEnrollment.put("SLA451", 100);
    }

    // Define a random number generator
    static Random random = new Random();

    // calculate the fitness of a given schedule
    static double calculateFitness(List<Schedule> schedule) {
        double fitness = 0.0;

        for (Schedule entry : schedule) {
            String activity = entry.activity;
            String room = entry.room;
            String facilitator = entry.facilitator;

            int roomCapacity = rooms.get(room);
            int enrollment = expectedEnrollment.get(activity);

            if (roomCapacity < enrollment) fitness -= 0.5;
            else if (roomCapacity > 3 * enrollment) fitness -= 0.2;
            else if (roomCapacity > 6 * enrollment) fitness -= 0.4;
            else fitness += 0.3;

            if (preferredFacilitators.get(activity).contains(facilitator)) fitness += 0.5;
            else if (Arrays.asList(facilitators).contains(facilitator)) fitness += 0.2;
            else fitness -= 0.1;
        }
        return fitness;
    }

    // generate a random schedule
    static List<Schedule> generateRandomSchedule() {
        List<Schedule> schedule = new ArrayList<>();
        for (String activity : activities) {
            String room = getRandomKey(rooms);
            String timeSlot = timeslots[random.nextInt(timeslots.length)];
            String facilitator = facilitators[random.nextInt(facilitators.length)];
            schedule.add(new Schedule(activity, room, timeSlot, facilitator));
        }
        return schedule;
    }

    // initialize a population of schedules
    static List<List<Schedule>> initializePopulation(int populationSize) {
        List<List<Schedule>> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            population.add(generateRandomSchedule());
        }
        return population;
    }

    // compute softmax values for an array of scores
    static double[] softmax(double[] scores) {
        double sum = 0.0;
        for (double score : scores) sum += Math.exp(score);
        double[] probabilities = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            probabilities[i] = Math.exp(scores[i]) / sum;
        }
        return probabilities;
    }

    // select population members using roulette wheel selection
    static List<List<Schedule>> rouletteWheelSelection(List<List<Schedule>> population, double[] scores) {
        double[] probabilities = softmax(scores);
        List<List<Schedule>> selectedPopulation = new ArrayList<>();
        for (int i = 0; i < population.size(); i++) {
            int index = selectIndex(probabilities);
            selectedPopulation.add(population.get(index));
        }
        return selectedPopulation;
    }

    // select an index based on probability
    static int selectIndex(double[] probabilities) {
        double randomValue = random.nextDouble();
        double cumulative = 0.0;
        for (int i = 0; i < probabilities.length; i++) {
            cumulative += probabilities[i];
            if (randomValue <= cumulative) return i;
        }
        return probabilities.length - 1;
    }

    //crossover between two schedules to create a new one
    static List<Schedule> crossover(List<Schedule> parent1, List<Schedule> parent2) {
        int crossoverPoint = random.nextInt(parent1.size() - 1) + 1;
        List<Schedule> child = new ArrayList<>(parent1.subList(0, crossoverPoint));
        child.addAll(parent2.subList(crossoverPoint, parent2.size()));
        return child;
    }

    // mutate a schedule with a given mutation rate
    static List<Schedule> mutate(List<Schedule> schedule, double mutationRate) {
        List<Schedule> mutatedSchedule = new ArrayList<>();
        for (Schedule entry : schedule) {
            if (random.nextDouble() < mutationRate) {
                String room = getRandomKey(rooms);
                String timeSlot = timeslots[random.nextInt(timeslots.length)];
                String facilitator = facilitators[random.nextInt(facilitators.length)];
                mutatedSchedule.add(new Schedule(entry.activity, room, timeSlot, facilitator));
            } else {
                mutatedSchedule.add(entry);
            }
        }
        return mutatedSchedule;
    }

    // gets a random key from a map
    static String getRandomKey(Map<String, Integer> map) {
        List<String> keys = new ArrayList<>(map.keySet());
        return keys.get(random.nextInt(keys.size()));
    }

    // Genetic algorithm method to find the best schedule
    static List<Schedule> geneticAlgorithm(int populationSize, double mutationRate, int generations) {
        List<List<Schedule>> population = initializePopulation(populationSize);
        List<Schedule> bestSchedule = null;
        double bestFitness = Double.NEGATIVE_INFINITY;

        for (int generation = 0; generation < generations; generation++) {
            double[] scores = population.stream().mapToDouble(GeneticScheduler::calculateFitness).toArray();
            List<List<Schedule>> selectedPopulation = rouletteWheelSelection(population, scores);
            List<List<Schedule>> nextGeneration = new ArrayList<>();

            for (int i = 0; i < selectedPopulation.size(); i += 2) {
                List<Schedule> parent1 = selectedPopulation.get(i);
                List<Schedule> parent2 = selectedPopulation.get((i + 1) % selectedPopulation.size());
                List<Schedule> child1 = mutate(crossover(parent1, parent2), mutationRate);
                List<Schedule> child2 = mutate(crossover(parent2, parent1), mutationRate);
                nextGeneration.add(child1);
                nextGeneration.add(child2);
            }
            population = nextGeneration;

            for (List<Schedule> schedule : population) {
                double fitness = calculateFitness(schedule);
                if (fitness > bestFitness) {
                    bestFitness = fitness;
                    bestSchedule = schedule;
                }
            }
        }
        return bestSchedule;
    }

    // Main method to run the genetic algorithm and print the best schedule
    public static void main(String[] args) {
        List<Schedule> bestSchedule = geneticAlgorithm(500, 0.01, 100);
        System.out.println("Best Schedule:");
        for (Schedule entry : bestSchedule) {
            System.out.println(entry.activity + " - " + entry.room + " - " + entry.timeSlot + " - " + entry.facilitator);
        }
    }

    // Schedule class to store details of an activity's scheduling information
    static class Schedule {
        String activity;
        String room;
        String timeSlot;
        String facilitator;

        Schedule(String activity, String room, String timeSlot, String facilitator) {
            this.activity = activity;
            this.room = room;
            this.timeSlot = timeSlot;
            this.facilitator = facilitator;
        }
    }
}

