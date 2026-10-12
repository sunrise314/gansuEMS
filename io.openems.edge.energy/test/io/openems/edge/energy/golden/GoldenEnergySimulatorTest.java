package io.openems.edge.energy.golden;

import static io.jenetics.engine.Limits.byFixedGeneration;
import static io.openems.edge.energy.api.EnergyUtils.filterEshsWithDifferentModes;
import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.jenetics.util.RandomRegistry;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.energy.api.Environment;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.DifferentModes;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation;
import io.openems.edge.energy.api.handler.DifferentModes.Modes.SingleModes;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.handler.OneMode;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Ess;
import io.openems.edge.energy.api.simulation.periods.Periods;
import io.openems.edge.energy.optimizer.ModeCombinations;
import io.openems.edge.energy.optimizer.SimulationResult;
import io.openems.edge.energy.optimizer.Simulator;
import io.openems.shared.golden.GoldenBaseline;
import io.openems.shared.golden.GoldenCheck;
import io.openems.shared.golden.GoldenJson;

/**
 * Golden data regression for the Energy {@link Simulator} (ch12: golden data
 * regression, algorithm layer).
 *
 * <p>
 * Four dataset categories target different failure modes. All scenarios use a
 * fixed UTC clock and a seeded {@link RandomRegistry} — output must be
 * bit-exactly reproducible. Baselines live in {@link GoldenEnergyBaselines};
 * regenerate via {@link GoldenEnergyBaselineGeneratorTest} and review manually
 * before committing baseline + code together.
 */
public class GoldenEnergySimulatorTest {

	/** Fixed instant: 1. January 2020 00:00:00 UTC — machine-independent. */
	private static final Instant INSTANT = Instant.ofEpochSecond(1577836800);
	private static final Clock CLOCK = Clock.fixed(INSTANT, ZoneOffset.UTC);
	private static final ZonedDateTime TIME = ZonedDateTime.ofInstant(INSTANT, ZoneOffset.UTC);

	private static final GlobalOptimizationContext.Grid GRID = new GlobalOptimizationContext.Grid(16000, 20000, 19000,
			io.openems.common.jscalendar.JSCalendar.Tasks.empty());

	private static final Ess ESS_DEFAULT = new Ess(5000, 22000, 16000, 16000);

	protected static enum Esh2State implements OptionsEnum {
		FOO(1, "Foo"), //
		BAR(2, "Bar");

		private final int value;
		private final String name;

		private Esh2State(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return this.value;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return FOO;
		}
	}

	public static final EnergyScheduleHandler.WithOnlyOneMode ESH0 = //
			new OneMode.Builder<Integer, Void>("Controller.Dummy", "esh0") //
					.setOptimizationContext(goc -> goc.ess().totalEnergy()) //
					.setSimulator((id, period, gsc, coc, csc, ef, fitness) -> {
						var minEnergy = socToEnergy(gsc.goc.ess().totalEnergy(), 10 /* [%] */);
						ef.setEssMaxDischarge(Math.max(0, gsc.ess.getInitialEnergy() - minEnergy));
					}) //
					.build();

	public static final EshWithDifferentModes<Esh2State, Void, Void> ESH2 = //
			new DifferentModes.Builder<Esh2State, Void, Void>("Controller.Dummy", "esh2") //
					.setModes(() -> new SingleModes<Esh2State>(null, Esh2State.values())) //
					.setInitialPopulationsProvider((goc, coc, modes) -> {
						return ImmutableSortedSet.of(new InitialPopulation<Esh2State>(goc.periods().stream() //
								.map(p -> p.index() % 3 == 0 //
										? Esh2State.BAR // set BAR mode
										: Esh2State.FOO) // default
								.toArray(Esh2State[]::new)));
					}) //
					.build();

	// ---------------------------------------------------------------- Scenarios

	/**
	 * REPLAY: a representative 12h day (48 quarter periods with varying prices and
	 * production), replayed with a fixed schedule. Detects systematic offsets.
	 */
	public static SimulationResult runReplayStandardDay() {
		var goc = goc(ESS_DEFAULT, replayPeriods());
		return SimulationResult.fromQuarters(goc, REPLAY_SCHEDULE, 0, 0);
	}

	private static final int[] REPLAY_SCHEDULE = { 1, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1,
			0, 1, 1, 0, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 1, 0, 0, 1 };

	private static Periods replayPeriods() {
		var b = Periods.builder(Environment.TEST);
		// 24 quarters with morning prices/production ramp...
		for (var i = 0; i < 24; i++) {
			b.addPeriodIfValid(time(0, 0).plusMinutes(15L * i), null, //
					production(0, 1200, i), 60 + (i % 5) * 10, price(280.0, 340.0, i), null);
		}
		// ...followed by 6 hours with midday volatility
		for (var i = 0; i < 24; i++) {
			b.addPeriodIfValid(time(6, 0).plusMinutes(15L * i), null, //
					production(2800, 200, i), 80 + (i % 4) * 15, price(220.0, 360.0, i), null);
		}
		return b.build();
	}

	private static int production(int start, int end, int i) {
		return start + (end - start) * i / 23;
	}

	private static double price(double start, double end, int i) {
		var swing = i % 2 == 0 ? 1.0 : -1.0;
		return start + (end - start) * i / 23.0 + swing * (i % 3) * 2.5;
	}

	/**
	 * BOUNDARY: SOC exactly at the lower limit (currentEnergy = 0 Wh) plus a grid
	 * buy soft limit exactly at maxBuyPower. Discharge must clamp to zero.
	 */
	public static SimulationResult runBoundarySocZero() {
		var periods = Periods.builder(Environment.TEST) //
				.addPeriodIfValid(time(0, 0), 16000 /* == maxBuyPower: edge */, 0, 1000, 300.0, null) //
				.addPeriodIfValid(time(0, 15), null, 0, 1000, 300.0, null) //
				.addPeriodIfValid(time(0, 30), null, 0, 1000, 300.0, null) //
				.addPeriodIfValid(time(0, 45), null, 0, 1000, 300.0, null) //
				.build();
		return SimulationResult.fromQuarters(goc(new Ess(0, 22000, 16000, 16000), periods), new int[] { 1, 1, 1, 1 }, 0,
				0);
	}

	/**
	 * EXTREME: extreme spot price volatility including deeply negative prices.
	 * Runs the full genetic engine with a seeded RNG (population 20, 10
	 * generations).
	 */
	public static SimulationResult runExtremePriceSwingDay() {
		var periods = Periods.builder(Environment.TEST) //
				.addPeriodIfValid(time(0, 0), null, 0, 500, -100.0, null) //
				.addPeriodIfValid(time(0, 15), null, 0, 500, -50.0, null) //
				.addPeriodIfValid(time(0, 30), null, 3000, 500, 3000.0, null) //
				.addPeriodIfValid(time(0, 45), null, 500, 500, 5000.0, null) //
				.addPeriodIfValid(time(1, 0), null, 2000, 500, -200.0, null) //
				.addPeriodIfValid(time(1, 15), null, 0, 500, 0.0, null) //
				.addPeriodIfValid(time(1, 30), null, 0, 500, 4000.0, null) //
				.addPeriodIfValid(time(1, 45), null, 1000, 500, 100.0, null) //
				.build();
		var simulator = new Simulator(goc(ESS_DEFAULT, periods));
		simulator.setEarliestCallbackDelay(Duration.ZERO);
		var result = new AtomicReference<SimulationResult>();
		simulator.runOptimization(//
				() -> SimulationResult.EMPTY_SIMULATION_RESULT, //
				false /* optimizeCurrentPeriod */, //
				engine -> engine //
						.populationSize(20), //
				stream -> stream //
						.limit(byFixedGeneration(10)), //
				result::set);
		return result.get();
	}

	/**
	 * INJECTION: schedule shorter than the period list — remaining periods must
	 * fall back to mode-combination index 0 gracefully.
	 */
	public static SimulationResult runInjectionShortSchedule() {
		var goc = goc(ESS_DEFAULT, fourQuarterPeriods(300.0));
		return SimulationResult.fromQuarters(goc, new int[] { 1 }, 0, 0);
	}

	/**
	 * INJECTION: empty schedule input must degrade to the empty result without
	 * throwing.
	 */
	public static SimulationResult runInjectionEmptySchedule() {
		var goc = goc(ESS_DEFAULT, fourQuarterPeriods(300.0));
		return SimulationResult.fromQuarters(goc, new int[] {}, 0, 0);
	}

	private static Periods fourQuarterPeriods(double price) {
		return Periods.builder(Environment.TEST) //
				.addPeriodIfValid(time(0, 0), null, 0, 1000, price, null) //
				.addPeriodIfValid(time(0, 15), null, 0, 1000, price, null) //
				.addPeriodIfValid(time(0, 30), null, 0, 1000, price, null) //
				.addPeriodIfValid(time(0, 45), null, 0, 1000, price, null) //
				.build();
	}

	// ------------------------------------------------------------------ Helpers

	private static ZonedDateTime time(int hours, int minutes) {
		return TIME.plusHours(hours).plusMinutes(minutes);
	}

	private static GlobalOptimizationContext goc(Ess ess, Periods periods) {
		final var eshs = ImmutableList.<EnergyScheduleHandler>of(ESH0, ESH2);
		return new GlobalOptimizationContext(//
				CLOCK, Environment.TEST, TIME, //
				eshs, //
				filterEshsWithDifferentModes(eshs).collect(ImmutableList.toImmutableList()), //
				GRID, ess, periods);
	}

	/**
	 * Serializes a {@link SimulationResult} to its golden canonical form.
	 *
	 * <p>
	 * Counters (simulationsCounter/generationsCounter) are intentionally excluded:
	 * they may vary with thread scheduling and carry no schedule semantics.
	 *
	 * @param r the {@link SimulationResult}
	 * @return golden JSON
	 */
	public static JsonObject goldenJson(SimulationResult r) {
		var root = new JsonObject();

		var f = r.fitness();
		var fj = new JsonObject();
		fj.addProperty("hardConstraintViolations", f.hardConstraintViolations());
		fj.add("constraintsAfterHardConstraintViolations", constraintsJson(f.constraintsAfterHardConstraintViolations()));
		fj.addProperty("gridBuyCostScore", f.gridBuyCostScore());
		fj.addProperty("gridBuyEnergyWh", f.gridBuyEnergyWh());
		fj.add("constraintsAfterGridBuyConstraints", constraintsJson(f.constraintsAfterGridBuyConstraints()));
		fj.addProperty("gridSellRevenueScore", f.gridSellRevenueScore());
		fj.addProperty("gridSellEnergyWh", f.gridSellEnergyWh());
		fj.addProperty("modePreferencePenalty", f.modePreferencePenalty());
		fj.addProperty("softConstraintViolations", f.softConstraintViolations());
		root.add("fitness", fj);

		var ess = r.ess();
		var ej = new JsonObject();
		ej.addProperty("currentEnergy", ess.currentEnergy());
		ej.addProperty("totalEnergy", ess.totalEnergy());
		ej.addProperty("maxChargePower", ess.maxChargePower());
		ej.addProperty("maxDischargePower", ess.maxDischargePower());
		root.add("ess", ej);

		var pj = new JsonObject();
		for (var e : r.periods().entrySet()) {
			var p = e.getValue();
			var v = new JsonObject();
			v.addProperty("modeIndex", p.modeCombination().index());
			var names = p.modeCombination().modes().stream() //
					.map(ModeCombinations.Mode::name) //
					.reduce((a, b2) -> a + "+" + b2) //
					.orElse("");
			v.addProperty("mode", names);
			v.addProperty("essInitialEnergy", p.essInitialEnergy());
			v.add("energyFlow", energyFlowJson(p.energyFlow()));
			pj.add(e.getKey().toString(), v);
		}
		root.add("periods", pj);

		var sj = new JsonObject();
		for (var e : r.schedules().entrySet()) {
			var eshj = new JsonObject();
			for (var e2 : e.getValue().entrySet()) {
				var tj = new JsonObject();
				tj.addProperty("modeIndex", e2.getValue().modeIndex());
				tj.addProperty("essInitialEnergy", e2.getValue().essInitialEnergy());
				eshj.add(e2.getKey().toString(), tj);
			}
			sj.add(e.getKey().id(), eshj);
		}
		root.add("schedules", sj);

		return root;
	}

	private static JsonArray constraintsJson(java.util.List<DifferentModes.AppliedCustomConstraint> constraints) {
		var a = new JsonArray();
		for (var c : constraints) {
			var o = new JsonObject();
			o.addProperty("name", c.constraint().name());
			o.addProperty("value", c.value());
			a.add(o);
		}
		return a;
	}

	private static JsonObject energyFlowJson(EnergyFlow ef) {
		var o = new JsonObject();
		o.addProperty("production", ef.getProduction());
		o.addProperty("unmanagedConsumption", ef.getUnmanagedConsumption());
		o.addProperty("consumption", ef.getConsumption());
		o.addProperty("ess", ef.getEss());
		o.addProperty("grid", ef.getGrid());
		var mc = new JsonObject();
		ef.getManagedConsumptions().entrySet().forEach(e -> mc.addProperty(e.getKey(), e.getValue()));
		o.add("managedConsumptions", mc);
		return o;
	}

	/**
	 * Initializes the seeded RNG — determinism precondition for all scenarios.
	 */
	public static void initializeRandomRegistryForGolden() {
		System.setProperty("io.jenetics.util.defaultRandomGenerator", "Random");
		RandomRegistry.random(new Random(123));
	}

	// ------------------------------------------------------- Golden assertions

	@org.junit.jupiter.api.Test
	void goldenReplayStandard12hDay() {
		assertScenario(GoldenEnergyBaselines.REPLAY_STANDARD_12H_DAY, runReplayStandardDay());
	}

	@org.junit.jupiter.api.Test
	void goldenBoundarySocZero() {
		assertScenario(GoldenEnergyBaselines.BOUNDARY_SOC_ZERO, runBoundarySocZero());
	}

	@org.junit.jupiter.api.Test
	void goldenExtremePriceSwingDay() {
		assertScenario(GoldenEnergyBaselines.EXTREME_PRICE_SWING_DAY, runExtremePriceSwingDay());
	}

	@org.junit.jupiter.api.Test
	void goldenInjectionShortSchedule() {
		assertScenario(GoldenEnergyBaselines.INJECTION_SHORT_SCHEDULE, runInjectionShortSchedule());
	}

	@org.junit.jupiter.api.Test
	void goldenInjectionEmptySchedule() {
		assertScenario(GoldenEnergyBaselines.INJECTION_EMPTY_SCHEDULE, runInjectionEmptySchedule());
	}

	private static void assertScenario(GoldenBaseline baseline, SimulationResult result) {
		GoldenCheck.assertMatches(baseline, goldenJson(result));
	}
}
