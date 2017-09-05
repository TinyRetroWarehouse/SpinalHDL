package spinal.lib.com.i2c

import spinal.core._
import spinal.lib.bus.amba3.apb.{Apb3SlaveFactory, Apb3, Apb3Config}
import spinal.lib.{master, slave}


object Apb3I2cSlave{
  def getApb3Config = Apb3Config(
    addressWidth = 8,
    dataWidth = 32,
    selWidth = 1,
    useSlaveError = false
  )


  def main(args: Array[String]) {
    SpinalVhdl(
      new Apb3I2cSlave(
        I2cSlaveMemoryMappedGenerics(
          I2cSlaveGenerics(
            samplingWindowSize = 3,
            samplingClockDividerWidth = 10 bits,
            timeoutWidth = 20 bits
          )
        )
      ).setDefinitionName("TopLevel")
    ).printPruned()
  }
}


case class Apb3I2cSlave(generics : I2cSlaveMemoryMappedGenerics) extends Component{
  val io = new Bundle{
    val apb =  slave(Apb3(Apb3I2cSlave.getApb3Config))
    val i2c = master(I2c())
    val interrupt = out Bool
  }

  val i2cCtrl = new I2cSlave(generics.ctrlGenerics)
  io.i2c <> i2cCtrl.io.i2c

  val busCtrl = Apb3SlaveFactory(io.apb)
  val bridge = i2cCtrl.io.driveFrom(busCtrl,0)(generics)
  io.interrupt := bridge.interruptCtrl.interrupt
}


