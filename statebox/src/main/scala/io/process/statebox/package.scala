package io

package object statebox {

  // TODO share between client / server
  type Marking = Map[Place, Set[Token]]
  type Token = Long
  type Transition = Long
  type Place = Long
}
