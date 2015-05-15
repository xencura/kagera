package io.process.statebox.common

import akka.actor._
import com.typesafe.config.Config

class Settings(config: Config) extends Extension {

  object Http {}
}

trait SettingsProvider {
  def settings: Settings
}

trait ActorSettingsProvider extends SettingsProvider {

  this: Actor =>

//  val settings = Settings(context.system)
}
