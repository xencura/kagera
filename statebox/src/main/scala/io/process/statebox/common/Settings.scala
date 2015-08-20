package io.process.statebox.common

import com.typesafe.config.{ Config, ConfigFactory }

class Settings(config: Config) {

  object http {
    val interface = config.getString("http.interface")
    val port = config.getInt("http.port")
  }
}

trait SettingsProvider {
  def settings: Settings
}

trait DefaultSettingsProvider extends SettingsProvider {

  override val settings = new Settings(ConfigFactory.load())
}
