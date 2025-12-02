/*
 * Copyright 2023-2025 The STARS Project Authors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tools.aqua.stars.logic.mtcq

import tools.aqua.stars.core.evaluation.BinaryPredicate.Companion.predicate
import tools.aqua.stars.core.evaluation.TSCEvaluation
import tools.aqua.stars.core.evaluation.UnaryPredicate.Companion.predicate
import tools.aqua.stars.core.metrics.evaluation.*
import tools.aqua.stars.core.tsc.builder.tsc
import tools.aqua.stars.data.av.dataclasses.*
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test

class MTCQTest {

    /** Lane for testing. */
    lateinit var lane: Lane

    /** Test setup. */
    @BeforeTest
    fun setup() {
        lane = Lane().also { Block(roads = listOf(Road(lanes = listOf(it)))) }
    }

    @Test
    fun testMTCQ() {
        val vehicle1 = Vehicle(id = 0, isEgo = true, lane = lane)
        val vehicle2 = Vehicle(id = 1, isEgo = false, lane = lane)
        val tickData1 = TickData(currentTick = TickDataUnitSeconds(0.0), entities = listOf(vehicle1, vehicle2))

        val changedVehicle1 = Vehicle(id = 0, isEgo = true, positionOnLane = 0.5, lane = lane)
        val changedVehicle2 = Vehicle(id = 1, isEgo = false, positionOnLane = 0.6, lane = lane)
        val tickData2 =
            TickData(currentTick = TickDataUnitSeconds(0.1), entities = listOf(changedVehicle1, changedVehicle2))

        val changedVehicle21 = Vehicle(id = 0, isEgo = true, positionOnLane = 1.0, lane = lane)
        val changedVehicle22 = Vehicle(id = 1, isEgo = false, positionOnLane = 2.0, lane = lane)
        val tickData3 =
            TickData(currentTick = TickDataUnitSeconds(0.2), entities = listOf(changedVehicle21, changedVehicle22))

        val changedVehicle31 = Vehicle(id = 0, isEgo = true, positionOnLane = 1.1, lane = lane)
        val changedVehicle32 = Vehicle(id = 1, isEgo = false, positionOnLane = 1.2, lane = lane)
        val tickData4 =
            TickData(currentTick = TickDataUnitSeconds(0.3), entities = listOf(changedVehicle31, changedVehicle32))

        val segment =
            Segment(
                segmentSource = "",
                tickData = listOf(tickData1, tickData2, tickData3, tickData4),
                simulationRunId = "1"
            )

        val soBetween =
            predicate(Vehicle::class to Vehicle::class) { _, v0, v1 ->
                v1.tickData.vehicles
                    .filter { it.id != v0.id && it.id != v1.id }
                    .any { vx ->
                        (v0.lane.uid == vx.lane.uid || v1.lane.uid == vx.lane.uid) &&
                                (v0.lane.uid != vx.lane.uid || (v0.positionOnLane < vx.positionOnLane)) &&
                                (v1.lane.uid != vx.lane.uid || (v1.positionOnLane > vx.positionOnLane))
                    }
            }

        val mtcqEval =
            MTCQEvaluator<Actor, TickDataUnitSeconds, Segment, TickDataUnitSeconds, TickDataDifferenceSeconds>()
        mtcqEval.loadOntology(File((MTCQEvaluator::class.java.getResource("/mtcqOntology.rdf")!!).toURI()))

        val dlConvertibleClasses = setOf(Lane::class, Vehicle::class)

        val testMtcqPred =
            predicate(Vehicle::class) { ctx, _ ->
                mtcq(
                    ctx,
                    "G(http://dlr.de/stars/mtcqTestOntology#MovableObject(?x) & http://dlr.de/stars/mtcqTestOntology#lane(?x,?y))",
                    mtcqEval,
                    dlConvertibleClasses = dlConvertibleClasses
                )
            }

        val myTsc = tsc<Actor, TickData, Segment, TickDataUnitSeconds, TickDataDifferenceSeconds> {
            all("TSC Root") {
                leaf("someone between") {
                    condition { ctx ->
                        ctx.segment.vehicleIds.any { v1 ->
                            soBetween.holds(ctx, ctx.segment.ticks.keys.first(), ctx.primaryEntityId, v1)
                        }
                    }
                }
                leaf("movable object always on same lane") { condition { ctx -> testMtcqPred.holds(ctx) } }
            }
        }

        TSCEvaluation(
            tscList = listOf(myTsc),
            writePlots = true,
            writePlotDataCSV = true,
            writeSerializedResults = true,
            compareToPreviousRun = false
        )
            .apply {
                registerMetricProviders(
                    TotalSegmentTickDifferencePerIdentifierMetric(),
                    SegmentCountMetric(),
                    TotalSegmentTickDifferenceMetric(),
                    InvalidTSCInstancesPerTSCMetric(),
                    MissedTSCInstancesPerTSCMetric(),
                )
                println("Run Evaluation")
                runEvaluation(segments = listOf(segment).asSequence())
            }
    }
}
