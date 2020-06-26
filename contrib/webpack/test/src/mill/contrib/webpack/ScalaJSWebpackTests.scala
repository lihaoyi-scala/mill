package mill.contrib.webpack

import ammonite.ops
import mill.define.Target
import mill.scalalib._
import mill.util.{TestEvaluator, TestUtil}
import mill.{Agg, T}
import os.ReadablePath
import utest.framework.TestPath
import utest.{TestSuite, Tests, _}

object ScalaJSWebpackTests extends TestSuite {

  trait WebpackTestModule extends TestUtil.BaseModule with ScalaJSWebpackModule {
      def scalaJSVersion: T[String] = "0.6.33"

      def scalaVersion: T[String] = "2.12.11"
  }

  object WebpackModuleSimple extends WebpackTestModule {
    override def npmDeps = Agg("uuid" -> "8.1.0")
  }

  object WebpackModuleTransitive extends WebpackTestModule {
    override def ivyDeps: Target[Agg[Dep]] = Agg(
      ivy"io.github.outwatch::outwatch::1.0.0-RC2",
    )
  }

  def webpackTest[T](m: TestUtil.BaseModule)(f: TestEvaluator => T)(
      implicit
      tp: TestPath): T = {
    val ev = new TestEvaluator(m)
    ops.rm(ev.outPath)
    f(ev)
  }

  val sProps: String =
    ops.read(os.resource / "snabbdom-custom-props.js")
  val npmSimpleJson: String =
    ops.read(os.resource / "package-simple.json")
  val npmTransitiveJson: String =
    ops.read(os.resource / "package-transitive.json")
  val wpConf: String => String = p =>
    s"""// Webpack config generated by ScalaJSWebpackModule
       |const generatedWebpackCfg = {
       |  "mode": "development",
       |  "devtool": "source-map",
       |  "entry": "out.js",
       |  "output": {
       |    "path": "$p",
       |    "filename": "out-bundle.js"
       |  }
       |};
       |
       |module.exports = generatedWebpackCfg;
       |""".stripMargin

  val mergedWithCustomWpConf: (String, String) => String = (outputPath, customConfigPath) =>
    s"""const merge = require('webpack-merge');
       |
       |// Webpack config generated by ScalaJSWebpackModule
       |const generatedWebpackCfg = {
       |  "mode": "development",
       |  "devtool": "source-map",
       |  "entry": "out.js",
       |  "output": {
       |    "path": "$outputPath",
       |    "filename": "out-bundle.js"
       |  }
       |};
       |
       |// Custom webpack config from '$customConfigPath', defined in ScalaJSWebpackModule
       |const customWebpackCfg = {
       |  module: {
       |    rules: [
       |      {
       |        use: [
       |          {
       |            loader: 'postcss-loader',
       |            options: {
       |              ident: 'postcss',
       |              plugins: [
       |                require('tailwindcss'),
       |                require('autoprefixer'),
       |              ],
       |            },
       |          },
       |        ],
       |      }
       |    ],
       |  }
       |};
       |
       |module.exports = merge(generatedWebpackCfg, customWebpackCfg);
       |""".stripMargin

  override def tests: Tests = Tests {
    "bundlerDeps" - {
      "extractFromJars" - webpackTest(WebpackModuleTransitive) { ev =>
        val Right((result, _)) = ev(WebpackModuleTransitive.jsDeps)

        assert(
          result.dependencies == Seq("snabbdom" -> "0.7.1"),
          result.devDependencies == Nil,
          result.jsSources get "snabbdom-custom-props.js" contains sProps,
        )
      }
    }

    "webpack" - {
      "writeJsSources" - webpackTest(WebpackModuleTransitive) { ev =>
        val Right((result, _)) = ev(WebpackModuleTransitive.writeBundleSources)

        result(
          JsDeps(jsSources = Map("snabbdom-custom-props.js" -> sProps)),
          ev.outPath)

        val src = ops.read(ev.outPath / "snabbdom-custom-props.js")

        assert(
          src == sProps,
        )
      }

      "writeCfg" - webpackTest(WebpackModuleTransitive) { ev =>
        val Right((result, _)) = ev(WebpackModuleTransitive.writeWpConfig)

        val configName = "webpack.config.foo.js"

        result(
          ev.outPath,
          configName,
          None,
          "out.js",
          "out-bundle.js",
          false)

        val cfg = ops.read(ev.outPath / configName)

        assert(
          cfg == wpConf(ev.outPath.toString),
        )
      }

      "writeWithCustomCfg" - webpackTest(WebpackModuleTransitive) { ev =>
        val Right((result, _)) = ev(WebpackModuleTransitive.writeWpConfig)

        result(
          ev.outPath,
          "webpack.config.js",
          Some(os.resource / "customWpConfig.js": ReadablePath),
          "out.js",
          "out-bundle.js",
          false
        )

        val cfg = ops.read(ev.outPath / "webpack.config.js")

        assert(
          cfg == mergedWithCustomWpConf(
            ev.outPath.toString,
            (os.resource / "customWpConfig.js").toString),
        )
      }

      "writePackageJson simple" - webpackTest(WebpackModuleSimple) { ev =>
        val Right((result, _)) = ev(WebpackModuleTransitive.writePackageSpec)

        result(JsDeps(List("uuid" -> "8.1.0")), ev.outPath)

        val pkg = ops.read(ev.outPath / "package.json")

        assert(pkg == npmSimpleJson)
      }

      "writePackageJson transitive" - webpackTest(WebpackModuleTransitive) { ev =>
        val Right((result, count)) = ev(WebpackModuleTransitive.writePackageSpec)

        result(JsDeps(List("snabbdom" -> "0.7.1")), ev.outPath)

        val pkg = ops.read(ev.outPath / "package.json")

        assert(pkg == npmTransitiveJson, count > 0)
      }
    }
  }
}
