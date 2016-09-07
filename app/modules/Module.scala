package modules

import com.google.inject.{AbstractModule, Provides}
import io.github.nthportal.paste.core.conf.{Conf, ConfProvider, PathConf, PathConfProvider}
import play.api.db.slick.DatabaseConfigProvider
import play.api.{Configuration, Environment}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.jdbc.JdbcBackend

class Module(env: Environment, cfg: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[PathConf]).toProvider(new PathConfProvider(cfg.getString("paste.internal.fs.base").get))
    bind(classOf[Conf]).toProvider(classOf[ConfProvider])
  }

  @Provides
  def provideDbConfig(dbConfigProvider: DatabaseConfigProvider): DatabaseConfig[JdbcProfile] = {
    dbConfigProvider.get[JdbcProfile]
  }

  @Provides
  def provideDb(dbConfig: DatabaseConfig[JdbcProfile]): JdbcBackend#DatabaseDef = dbConfig.db
}
