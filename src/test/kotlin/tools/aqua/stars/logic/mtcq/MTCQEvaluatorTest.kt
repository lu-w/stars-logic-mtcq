package tools.aqua.stars.logic.mtcq

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import tools.aqua.stars.core.evaluation.BinaryPredicate.Companion.predicate
import tools.aqua.stars.core.evaluation.PredicateContext
import tools.aqua.stars.core.evaluation.TSCEvaluation
import tools.aqua.stars.core.evaluation.UnaryPredicate.Companion.predicate
import tools.aqua.stars.core.metrics.evaluation.*
import tools.aqua.stars.core.tsc.builder.tsc
import tools.aqua.stars.data.av.dataclasses.*
import tools.aqua.stars.data.av.dataclasses.TickData
import java.io.File

class MTCQEvaluatorTest {

    private lateinit var lane: Lane
    private lateinit var vehicle1: Vehicle
    private lateinit var vehicle2: Vehicle
    private var mtcqEval = MTCQEvaluator<Actor, TickDataUnitSeconds, Segment, TickDataUnitSeconds, TickDataDifferenceSeconds>()

    @BeforeTest
    fun setup() {
        lane = Lane().also { Block(roads = listOf(Road(lanes = listOf(it)))) }

        vehicle1 = Vehicle(id = 0, isEgo = true, lane = lane)
        vehicle2 = Vehicle(id = 1, isEgo = false, lane = lane)

        mtcqEval.loadOntology(File((MTCQEvaluator::class.java.getResource("/mtcqOntology.rdf")!!).toURI()))
    }

    private fun createTickData(vararg positions: Pair<Vehicle, Double>): TickData {
        val updatedVehicles = positions.map { (v, pos) ->
            v.copy(positionOnLane = pos)
        }
        return TickData(currentTick = TickDataUnitSeconds(0.0), entities = updatedVehicles)
    }

    @Test
    fun `MTCQ predicate evaluates vehicle on same lane`() {
        val dlConvertibleClasses = setOf(Lane::class, Vehicle::class)
        val testMtcqPred = predicate(Vehicle::class) { ctx, _ ->
            mtcq(
                ctx,
                "G(http://dlr.de/stars/mtcqTestOntology#MovableObject(?x) & http://dlr.de/stars/mtcqTestOntology#lane(?x,?y))",
                mtcqEval,
                dlConvertibleClasses = dlConvertibleClasses
            )
        }

        val tickData = createTickData(vehicle1 to 0.0, vehicle2 to 0.0)
        val segment = Segment(segmentSource = "", tickData = listOf(tickData), simulationRunId = "1")
        val context = PredicateContext(segment)

        assertTrue { testMtcqPred.holds(context) }
    }

    @Test
    fun `MTCQ predicate evaluates no non movable object`() {
        val dlConvertibleClasses = setOf(Lane::class, Vehicle::class)
        val testMtcqPred = predicate(Vehicle::class) { ctx, _ ->
            mtcq(
                ctx,
                "G(!http://dlr.de/stars/mtcqTestOntology#MovableObject(?x))",
                mtcqEval,
                dlConvertibleClasses = dlConvertibleClasses
            )
        }

        val tickData = createTickData(vehicle1 to 0.0, vehicle2 to 0.0)
        val segment = Segment(segmentSource = "", tickData = listOf(tickData), simulationRunId = "1")
        val context = PredicateContext(segment)

        assertFalse { testMtcqPred.holds(context) }
    }

    @Test
    fun `TSC evaluation runs without exceptions`() {
        val tickData = createTickData(vehicle1 to 0.0, vehicle2 to 0.0)
        val segment = Segment(segmentSource = "", tickData = listOf(tickData), simulationRunId = "1")

        val myTsc = tsc<Actor, TickData, Segment, TickDataUnitSeconds, TickDataDifferenceSeconds> {
            all("TSC Root") {
                leaf("movable object always on same lane") { condition { ctx -> true } }
            }
        }

        val evaluation = TSCEvaluation(
            tscList = listOf(myTsc),
            writePlots = false,
            writePlotDataCSV = false,
            writeSerializedResults = false,
            compareToPreviousRun = false
        ).apply {
            registerMetricProviders(
                TotalSegmentTickDifferencePerIdentifierMetric(),
                SegmentCountMetric(),
                TotalSegmentTickDifferenceMetric(),
                InvalidTSCInstancesPerTSCMetric(),
                MissedTSCInstancesPerTSCMetric(),
            )
        }

        // Ensure evaluation runs
        evaluation.runEvaluation(segments = listOf(segment).asSequence())
    }
}
