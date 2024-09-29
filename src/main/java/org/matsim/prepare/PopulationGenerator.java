package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.prepare.population.Attributes;
import org.matsim.prepare.population.EnumeratedAttributeDistribution;
import org.matsim.prepare.population.UniformAttributeDistribution;
import org.yaml.snakeyaml.reader.ReaderException;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.IntStream;

@SuppressWarnings({"all"})
/**
 * Test.
 */
public final class PopulationGenerator {
	private static final NumberFormat FMT = NumberFormat.getInstance(Locale.GERMAN);
	private static final Logger log = LogManager.getLogger(PopulationGenerator.class);


	public PopulationGenerator() {
	}

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("C:\\Users\\Benedictus\\IdeaProjects\\matsim-berlin\\input\\v6.3\\berlin-v6.3.config-ben.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();
		PopulationFactory popFactory = population.getFactory();
		final Coord stadium = new Coord(787799.747624, 5826731.166970);
		final Coord fanmeileKudamm = new Coord(796297.5093840164, 5827331.951002251);
		final Coord fanmeileBBTor = new Coord(794332.651325, 5826009.941581);
		List<String> modes = new ArrayList<>();
		modes.add("car");
		modes.add("pt");

		List<Coord> startCoords = readFile("G:\\TU Stuff\\MatSim\\HA2\\ImportHotel.txt");
		var destination = new EnumeratedAttributeDistribution<>(Map.of(stadium, 0.65, fanmeileKudamm, 0.25,fanmeileBBTor,0.1));
		/*List<Coord> destinationCoords = new ArrayList<Coord>();
		destinationCoords.add(stadium);
		destinationCoords.add(fanmeileKudamm);
		destinationCoords.add(fanmeileBBTor);*/

		int gesamtEinwohner = 3645000;

		double young = 19 / 100;
		double old = 19 / 100;

		// x women for 100 men
		double quota = 0.48;

		// sometimes this entry is not set
		double unemployed;
		unemployed = 3 / 100;

		var sex = new EnumeratedAttributeDistribution<>(Map.of("f", quota, "m", 1 - quota));
		var employment = new EnumeratedAttributeDistribution<>(Map.of(true, 1 - unemployed, false, unemployed));
		var ageGroup = new EnumeratedAttributeDistribution<>(Map.of(
			PopulationGenerator.AgeGroup.YOUNG, young,
			PopulationGenerator.AgeGroup.MIDDLE, 1.0 - young - old,
			PopulationGenerator.AgeGroup.OLD, old
		));


		//MultiPolygon geom = lors.get(raumID);

		var youngDist = new UniformAttributeDistribution<>(IntStream.range(1, 18).boxed().toList());
		var middleDist = new UniformAttributeDistribution<>(IntStream.range(18, 65).boxed().toList());
		var oldDist = new UniformAttributeDistribution<>(IntStream.range(65, 100).boxed().toList());


		for (int i = 0; i < 2000; i++) {
			Random r = new Random();

			/*Sets the basic Information of the Person (Start, Destination, ID). The Start comes from a List of Hotels, and the Destinations can be
			randomly selected from the three*/
			Coord fromCoord = randomizer(startCoords);
			Coord toCoord = destination.sample();
			String mode = modes.get(r.nextInt(modes.size()));
			String id = "visitor_" + i;

			Person person = createPerson(fromCoord, toCoord, mode, id, popFactory);
			PersonUtils.setSex(person, sex.sample());
			PopulationUtils.putSubpopulation(person, "person");

			PopulationGenerator.AgeGroup group = ageGroup.sample();

			if (group == PopulationGenerator.AgeGroup.MIDDLE) {
				PersonUtils.setAge(person, middleDist.sample());
				PersonUtils.setEmployed(person, employment.sample());
			} else if (group == PopulationGenerator.AgeGroup.YOUNG) {
				PersonUtils.setAge(person, youngDist.sample());
				PersonUtils.setEmployed(person, false);
			} else if (group == PopulationGenerator.AgeGroup.OLD) {
				PersonUtils.setAge(person, oldDist.sample());
				PersonUtils.setEmployed(person, false);
			}


			person.getAttributes().putAttribute(Attributes.HOME_X, fromCoord.getX());
			person.getAttributes().putAttribute(Attributes.HOME_Y, fromCoord.getY());

			person.getAttributes().putAttribute(Attributes.GEM, 11000000);
			person.getAttributes().putAttribute(Attributes.ARS, 110000000000L);

			population.addPerson(person);
		}

		PopulationUtils.writePopulation(population, "G:\\TU Stuff\\MatSim\\HA2\\berlin-v6.3-10pct_bearbeitet.plans.xml.gz");

	}

	private static Person createPerson(Coord home, Coord work, String mode, String id, PopulationFactory factory) {

		// create a person by using the population's factory
		// The only required argument is an id
		Person person = factory.createPerson(Id.createPersonId(id));
		Plan plan = createPlan(home, work, mode, factory);
		person.addPlan(plan);
		return person;
	}

	private static Plan createPlan(Coord home, Coord work, String mode, PopulationFactory factory) {


		Plan plan = factory.createPlan();

		Activity hotelActivityInTheMorning = factory.createActivityFromCoord("hotel", home);
		hotelActivityInTheMorning.setEndTime(61200);
		plan.addActivity(hotelActivityInTheMorning);

		Leg toWork = factory.createLeg(mode);
		plan.addLeg(toWork);

		Activity leisureActivity = factory.createActivityFromCoord("football", work);
		leisureActivity.setStartTime(72000);
		leisureActivity.setEndTime(79000);
		plan.addActivity(leisureActivity);

		Leg toHome = factory.createLeg(mode);
		plan.addLeg(toHome);

		Activity hotelActivityInTheEvening = factory.createActivityFromCoord("hotel", home);
		plan.addActivity(hotelActivityInTheEvening);

		return plan;
	}

	private static List<Coord> readFile(String fileLocation) {
		List<Coord> map = new ArrayList<>();
		try {
			File filename = new File(fileLocation);
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String info = null;
			//int counter = 0;
			while ((info = reader.readLine()) != null) {
				String[] parts = info.split(";");

				var coordinates = new Coord(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));

				map.add(coordinates);

			}
		} catch (ReaderException e) {
			System.out.println(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return map;
	}

	private static Coord randomizer(List<Coord> coordList) {
		Random random = new Random();
		int index = random.nextInt(coordList.size());

		return coordList.get(index);
	}

	private enum AgeGroup {
		YOUNG,
		MIDDLE,
		OLD
	}
}
