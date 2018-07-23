/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hal.tokyo.rd4c.boccodebook;

import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gn5r
 */
public class PlayButtonListener implements GpioPinListenerDigital {

    private final BGMPlayer bgmPlayer;
    private final Main main;

    public PlayButtonListener(BGMPlayer bgmPlayer) {
        this.bgmPlayer = bgmPlayer;

        this.main = new Main();
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent gpdsce) {
        if (gpdsce.getState() == PinState.LOW) {
            /* BGM 停止 */
            this.bgmPlayer.stopBGM();

            try {
                /*    Boccoへ最新メッセージ送信    */
                this.main.recentlySend();
            } catch (Exception e) {
                e.printStackTrace();
            }

            /* BGM　再開 */
            this.bgmPlayer.start();
        }
    }

}
