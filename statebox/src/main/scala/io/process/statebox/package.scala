package io.process

package object statebox {

  type Id = Long
  type ProcessModel = String
  type Marking = Set[Token]
  type Token = (Id, Any)
  type Transition = Long
  type Place = Long
}
