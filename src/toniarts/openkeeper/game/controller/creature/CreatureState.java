/*
 * Copyright (C) 2014-2016 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.controller.creature;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;

/**
 * State machine for creature AI. TODO: needs to be hierarchical so that this
 * class doesn't grow to be millions of lines
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public enum CreatureState implements State<ICreatureStateController> {

    IDLE() {

        @Override
        public void enter(ICreatureStateController entity) {

            // Should we flee or attack
            if (entity.shouldFleeOrAttack()) {
                return;
            }

            // Idling is the last resort
            entity.unassingCurrentTask();
            if (!findStuffToDo(entity)) {
                entity.navigateToRandomPoint();
            }
        }

        private boolean findStuffToDo(ICreatureStateController entity) {

            // See if we should just follow
            if (entity.getParty() != null && !entity.getParty().isPartyLeader(entity)) {
                entity.setFollowTarget(entity.getParty().getPartyLeader().getEntityId());
                entity.changeState(CreatureState.FOLLOW);
                return true;
            }

            // See if we have an objective
            if (entity.hasObjective() && entity.followObjective()) {
                entity.changeState(CreatureState.WORK);
                return true;
            }

            // See lair need
            if (entity.needsLair() && !entity.hasLair() && entity.findLair()) {
                entity.changeState(CreatureState.WORK);
                return true; // Found work
            }

            // See basic needs
            if (entity.hasLair() && entity.isNeedForSleep() && entity.goToSleep()) {
                entity.changeState(CreatureState.WORK);
                return true; // Found work
            }

            // See hunger
            if (entity.isHungry() && entity.goToEat()) {
                entity.changeState(CreatureState.WORK);
                return true; // Found work
            }

            // Find work
            if (entity.findWork() || (entity.isWorker() && entity.isTooMuchGold() && entity.dropGoldToTreasury())) {
                entity.changeState(CreatureState.WORK);
                return true; // Found work
            }

            return false;
        }

        @Override
        public void update(ICreatureStateController entity) {

            // Should we flee or attack
            if (entity.shouldFleeOrAttack()) {
                return;
            }

            if (entity.isTimeToReEvaluate()) {
                entity.resetReEvaluationTimer();
                if (!findStuffToDo(entity) && entity.isStopped()) {
                    entity.navigateToRandomPoint();
                }
            }
        }

        @Override
        public void exit(ICreatureStateController entity) {

        }

        @Override
        public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }
    },
    WANDER() {

        @Override
        public void enter(ICreatureStateController entity) {
//                    entity.wander();
        }

        @Override
        public void update(ICreatureStateController entity) {
//                    if (entity.idleTimeExceeded()) {
//                        entity.changeState(IDLE);
//                    }
        }

        @Override
        public void exit(ICreatureStateController entity) {

        }

        @Override
        public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }

    },
    DEAD() {
        @Override
        public void enter(ICreatureStateController entity) {
            //entity.die();
        }

        @Override
        public void update(ICreatureStateController entity) {
//                    if (entity.idleTimeExceeded()) {
//                        entity.changeState(IDLE);
//                    }
        }

        @Override
        public void exit(ICreatureStateController entity) {

        }

        @Override
        public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }
    }, SLAPPED {

        @Override
            public void enter(ICreatureStateController entity) {

        }

        @Override
            public void update(ICreatureStateController entity) {

        }

        @Override
            public void exit(ICreatureStateController entity) {

        }

        @Override
            public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }
    }, WORK {

        @Override
            public void enter(ICreatureStateController entity) {
            entity.navigateToAssignedTask();
        }

        @Override
            public void update(ICreatureStateController entity) {

            // Should we flee or attack
            if (entity.shouldFleeOrAttack()) {
                return;
            }

            // Check validity
            // If we have some pocket money left, we should return it to treasury
            if (!entity.isAssignedTaskValid() && !entity.dropGoldToTreasury()) {
                entity.changeState(IDLE);
                return;
            }

            // Check arrival
            if (entity.isAtAssignedTaskTarget()) {

                // Do the task we were supposed to do
                entity.executeAssignedTask();

                // If we have too much gold, drop it to the treasury
                if (entity.isTooMuchGold()) {
                    if (!entity.dropGoldToTreasury()) {
                        entity.dropGold();
                    }
                }
            } else if (entity.isStopped()) {
                entity.navigateToAssignedTask();
            }
        }

        @Override
            public void exit(ICreatureStateController entity) {
            entity.unassingCurrentTask();
        }

        @Override
            public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }

    }, FIGHT {

        @Override
            public void enter(ICreatureStateController entity) {
            entity.unassingCurrentTask();
        }

        @Override
            public void update(ICreatureStateController entity) {
            ICreatureController attackTarget = entity.getAttackTarget();
            if (attackTarget == null) {
                entity.changeState(IDLE); // Nothing to do
                return;
            }

            // If we have reached the target, stop and fight!
            if (entity.isWithinAttackDistance(attackTarget.getEntityId())) {

                // Attack!!
                entity.stopCreature();
                entity.executeAttack(attackTarget.getEntityId());
            } else {
                entity.navigateToAttackTarget(attackTarget.getEntityId());
            }
        }

        @Override
            public void exit(ICreatureStateController entity) {

        }

        @Override
            public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }

    }, FOLLOW {

        @Override
            public void enter(ICreatureStateController entity) {
            //entity.followTarget(entity.getFollowTarget());
        }

        @Override
            public void update(ICreatureStateController entity) {

            // See if we should follow
            if (entity.getFollowTarget() == null || entity.getFollowTarget().isIncapacitated()) {
                entity.changeState(IDLE);
                return;
            }

            // Should we flee or attack
            if (entity.shouldFleeOrAttack()) {
                return;
            }

            // If leader has set a task, perform it
            if (entity.getAssignedTask() != null) {
                entity.changeState(WORK);
                return;
            }

            // Don't let the target wander too far off
            if (entity.shouldNavigateToFollowTarget()) {
                entity.navigateToRandomPointAroundTarget(entity.getFollowTarget().getEntityId(), 1);
            }
        }

        @Override
            public void exit(ICreatureStateController entity) {
            entity.resetFollowTarget();
            entity.stopCreature();
        }

        @Override
            public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }

    },
    ENTERING_DUNGEON {

        @Override
        public void enter(ICreatureStateController entity) {

        }

        @Override
        public void update(ICreatureStateController entity) {
            if (entity.isStateTimeExceeded()) {
                entity.changeState(IDLE);
            }
        }

        @Override
        public void exit(ICreatureStateController entity) {

        }

        @Override
        public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }

    },
    PICKED_UP {

        @Override
        public void enter(ICreatureStateController entity) {

        }

        @Override
        public void update(ICreatureStateController entity) {

        }

        @Override
        public void exit(ICreatureStateController entity) {

        }

        @Override
        public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }

    }, FLEE {

        @Override
            public void enter(ICreatureStateController entity) {
            entity.unassingCurrentTask();
            entity.flee();
        }

        @Override
            public void update(ICreatureStateController entity) {
            if (!entity.shouldFleeOrAttack()) {
                entity.changeState(IDLE);
            }
        }

        @Override
            public void exit(ICreatureStateController entity) {

        }

        @Override
            public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }

    }, UNCONSCIOUS {
        @Override
            public void enter(ICreatureStateController entity) {
            entity.stopCreature();
            entity.unassingCurrentTask();
        }

        @Override
            public void update(ICreatureStateController entity) {

        }

        @Override
            public void exit(ICreatureStateController entity) {

        }

        @Override
            public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }

    }, STUNNED {

        @Override
            public void enter(ICreatureStateController entity) {

        }

        @Override
            public void update(ICreatureStateController entity) {

        }

        @Override
            public void exit(ICreatureStateController entity) {

        }

        @Override
            public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }
    }, IMPRISONED {

        @Override
            public void enter(ICreatureStateController entity) {

        }

        @Override
            public void update(ICreatureStateController entity) {

        }

        @Override
            public void exit(ICreatureStateController entity) {

        }

        @Override
            public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }
    }, TORTURED {

        @Override
            public void enter(ICreatureStateController entity) {

        }

        @Override
            public void update(ICreatureStateController entity) {

        }

        @Override
            public void exit(ICreatureStateController entity) {

        }

        @Override
            public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }
    }, SLEEPING {

        @Override
            public void enter(ICreatureStateController entity) {

        }

        @Override
            public void update(ICreatureStateController entity) {
            if (entity.isAttacked() || entity.isEnoughSleep()) {
                entity.changeState(IDLE);
            }
        }

        @Override
            public void exit(ICreatureStateController entity) {
            entity.stopRecuperating();
        }

        @Override
            public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }
    }, RECUPERATING {

        @Override
            public void enter(ICreatureStateController entity) {

        }

        @Override
            public void update(ICreatureStateController entity) {
            if (entity.isFullHealth()) {
                entity.changeState(IDLE);
            }
        }

        @Override
            public void exit(ICreatureStateController entity) {
                entity.stopRecuperating();
        }

        @Override
            public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }
    }, DRAGGED {

        @Override
            public void enter(ICreatureStateController entity) {

            }

            @Override
            public void update(ICreatureStateController entity) {

            }

            @Override
            public void exit(ICreatureStateController entity) {

            }

            @Override
            public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
                return true;
        }
    }, FALLEN {

        @Override
            public void enter(ICreatureStateController entity) {

        }

        @Override
            public void update(ICreatureStateController entity) {
            if (entity.isStateTimeExceeded()) {
                entity.changeState(GETTING_UP);
            }
        }

        @Override
            public void exit(ICreatureStateController entity) {

        }

        @Override
            public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }
    }, GETTING_UP {

        @Override
            public void enter(ICreatureStateController entity) {

            }

            @Override
            public void update(ICreatureStateController entity) {
                if (entity.isStateTimeExceeded()) {
                    entity.changeState(IDLE);
                }
            }

            @Override
            public void exit(ICreatureStateController entity) {

            }

            @Override
            public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
                return true;
        }
    }, MELEE_ATTACK {

        @Override
            public void enter(ICreatureStateController entity) {

        }

        @Override
            public void update(ICreatureStateController entity) {
            if (entity.isStateTimeExceeded()) {
                entity.changeState(FIGHT);
            }
        }

        @Override
            public void exit(ICreatureStateController entity) {

        }

        @Override
            public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }
    }, EATING {

        @Override
            public void enter(ICreatureStateController entity) {

        }

        @Override
            public void update(ICreatureStateController entity) {
            if (entity.isStateTimeExceeded()) {
                entity.sate();

                // Should we flee or attack
                if (entity.shouldFleeOrAttack()) {
                    return;
                }

                // See if we are still hungry
                if (entity.isHungry() && entity.goToEat()) {
                    entity.changeState(CreatureState.WORK);
                    return;
                }
                entity.changeState(CreatureState.IDLE);
            }

        }

        @Override
            public void exit(ICreatureStateController entity) {

        }

        @Override
            public boolean onMessage(ICreatureStateController entity, Telegram telegram) {
            return true;
        }
    }
}
