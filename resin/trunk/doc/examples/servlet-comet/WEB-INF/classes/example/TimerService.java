package example;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import javax.annotation.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.servlets.comet.*;

public class TimerService implements Runnable {
  @Resource ScheduledExecutorService _timer;

  private Future _timerFuture;
  
  private ArrayList<CometState> _stateList
    = new ArrayList<CometState>();

  public TimerService(ScheduledExecutorService timer)
  {
    _timer = timer;
    
    _timerFuture = _timer.scheduleAtFixedRate(this, 0, 2, TimeUnit.SECONDS);
  }

  public void addCometState(CometState state)
  {
    synchronized (_stateList) {
      _stateList.add(state);
    }
  }

  /**
   * The timer task wakes up every active comet state.
   *
   * A more sophisticated application would notify the comet states
   * as part of an event-based system.
   */
  public void run()
  {
    synchronized (_stateList) {
      for (int i = _stateList.size() - 1; i >= 0; i--) {
        CometState state = _stateList.get(i);

        if (! state.wake())
          _stateList.remove(i);
      }
    }
  }

  /**
   * Close the timer.
   */
  public void close()
  {
    _timerFuture.cancel(false);
  }
}
