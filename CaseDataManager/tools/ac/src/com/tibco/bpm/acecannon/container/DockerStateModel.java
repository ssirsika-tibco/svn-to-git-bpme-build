/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockerStateModel
{
	public enum Action
	{
		START,
		STOP,
		PAUSE,
		UNPAUSE,
		REMOVE,
		CLONE
	}

	private Map<String, List<Action>> actionMap;

	private void initActionMap()
	{
		actionMap = new HashMap<String, List<Action>>();
		actionMap.put("running", Arrays.asList(Action.STOP, Action.PAUSE, Action.CLONE));
		actionMap.put("created", Arrays.asList(Action.START, Action.REMOVE, Action.CLONE));
		actionMap.put("paused", Arrays.asList(Action.UNPAUSE, Action.CLONE));
		actionMap.put("exited", Arrays.asList(Action.START, Action.REMOVE, Action.CLONE));
		actionMap.put("restarting", Arrays.asList(Action.STOP, Action.REMOVE, Action.CLONE));
	}

	public DockerStateModel()
	{
		initActionMap();
	}

	public List<Action> getActionsAllowedInState(String state)
	{
		return actionMap.get(state);
	}

	// Gets the actions that are legal for ALL of the supplied states
	// (i.e. the intersection of their individual legal actions)
	public List<Action> getActionsAllowedInStates(Collection<String> states)
	{
		List<Action> workingList = null;

		for (String state : states)
		{
			List<Action> actionsAllowedInState = getActionsAllowedInState(state);
			if (workingList == null)
			{
				// First time through, so copy all actions
				workingList = new ArrayList<Action>();
				workingList.addAll(actionsAllowedInState);
			}
			else
			{
				// 2nd or subsequent state, so remove any actions in the
				// working list that are NOT allowed for this state
				workingList.removeIf(a -> !actionsAllowedInState.contains(a));
				//				for (int i = 0; i < workingList.size();)
				//				{
				//					if (!actionsAllowedInState.contains(workingList.get(i)))
				//					{
				//						// A state in the working list is not allowed for
				//						// THIS state, so remove it
				//						workingList.remove(i);
				//						// No need to i++, as we've just removed something
				//					}
				//					else
				//					{
				//						i++;
				//					}
				//				}
			}
		}
		return workingList;
	}
}
