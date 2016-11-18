/******************************************************************************
  * Triple DES (3DES)
  *
  *           Encrpytion :                Decrytpion :
  *
  *           plaintext                   plaintext      (64 bits)
  *               |                           |
  *       -----------------            ----------------
  *      |   DES encrypt   |<-- K1 -->|  DES decrypt   |
  *       -----------------            ----------------
  *               |                           |
  *       -----------------            ----------------
  *      |   DES decrypt   |<-- K2 -->|  DES encrypt   |
  *       -----------------            ----------------
  *               |                           |
  *       -----------------            ----------------
  *      |   DES encrypt   |<-- K3 -->|  DES decrypt   |
  *       -----------------            ----------------
  *               |                           |
  *           ciphertext                   ciphertext      (64 bits)
  *
  *
  *    key = Concatenation(k1 , k2 , k3) = 3*64 bits = 192 bits
  *
  */

package spinal.lib.crypto.symmetric

import spinal.core._
import spinal.lib.fsm._


class TripleDESBlock() extends Component{

  val gDES = DESBlockGenerics()
  val gIO  = SymmetricCryptoBlockGeneric(keyWidth    = ((gDES.keyWidth.value + gDES.keyWidthParity.value) * 3) bits, // TODO remove .value
                                         blockWidth  = gDES.blockWidth,
                                         useEncDec   = true)

  val io = new SymmetricCryptoBlockIO(gIO)

  val block    = Reg(Bits(gDES.blockWidth))

  // DES Block
  val blockDES = new DESBlock()


  /**
    * Triple DES state machine
    */
  val sm3DES = new StateMachine{

    val desCmdValid = False
    val desEncDec   = False
    val desKey      = B(0, gDES.keyWidthParity + gDES.keyWidth)
    val inSel       = False
    val cmdReady    = False

    val sIdle : State = new State with EntryPoint{
      whenIsActive{
        when(io.cmd.valid){
          goto(sStage1)
        }
      }
    }

    val sStage1 : State = new State{
      whenIsActive{
        desEncDec   := io.cmd.encDec ? True | False
        desCmdValid := True
        desKey      := io.cmd.encDec ? io.cmd.key(191 downto 128) | io.cmd.key(63 downto 0)

        when(blockDES.io.rsp.valid){
          desCmdValid := False
          block       := blockDES.io.rsp.block
          goto(sStage2)
        }
      }
    }

    val sStage2 : State = new State{
      whenIsActive{
        inSel       := True
        desEncDec   := io.cmd.encDec ? False | True
        desKey      := io.cmd.key(127 downto 64)
        desCmdValid := True

        when(blockDES.io.rsp.valid){
          desCmdValid := False
          block       := blockDES.io.rsp.block
          goto(sStage3)
        }
      }
    }

    val sStage3 : State = new State{
      whenIsActive{
        inSel       := True
        desEncDec   := io.cmd.encDec ? True | False
        desKey      := io.cmd.encDec ? io.cmd.key(63 downto 0) | io.cmd.key(191 downto 128)
        desCmdValid := True

        when(blockDES.io.rsp.valid){
          desCmdValid := False
          cmdReady    := True
          block       := blockDES.io.rsp.block
          goto(sIdle)
        }
      }
    }
  }


  /**
    * Des block connection
    */
  blockDES.io.cmd.valid  <> sm3DES.desCmdValid
  blockDES.io.cmd.key    <> sm3DES.desKey
  blockDES.io.cmd.encDec <> sm3DES.desEncDec
  blockDES.io.cmd.block  <> (sm3DES.inSel ? block | io.cmd.block)


  /**
    * Output
    */
  io.rsp.block := block
  io.rsp.valid := sm3DES.cmdReady
  io.cmd.ready := sm3DES.cmdReady
}


