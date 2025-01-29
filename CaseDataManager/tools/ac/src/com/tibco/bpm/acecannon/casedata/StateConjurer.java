/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.tibco.bpm.da.dm.api.State;
import com.tibco.bpm.da.dm.api.StateModel;
import com.tibco.bpm.da.dm.api.StructuredType;

public class StateConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	public static Option	optionAvoidTerminal	= new Option(OptionType.BOOLEAN, "avoidTerminal",
			"Avoid terminal states");

	public static Option	optionDistribution	= new ChoiceOption("distribution", "Distribution",
			Arrays.asList("even", "descending", "ascending"),
			Arrays.asList("Even", "Descending", "Ascending"));

	@Override
	public List<Option> getOptions()
	{
		return Arrays.asList(optionDistribution, optionAvoidTerminal);
	}

	private StateModel stateModel;

	public StateConjurer(StructuredType type)
	{
		this.stateModel = type.getStateModel();
	}

	// Sum of integers 1..size
	public static int sumSeries(int size)
	{
		return ((size * (size + 1)) / 2);
	}

	// Return (zero-based) index of chosen item
	public static int choose(int numItems)
	{
		int sum = sumSeries(numItems);
		int rand = ThreadLocalRandom.current().nextInt(sum);

		int t = numItems - 1;
		while (sumSeries(t) > rand)
		{
			t--;
		}
		return t;
	}

	@Override
	public String conjure()
	{
		List<String> states = null;
		Boolean avoidTerminal = (Boolean) getOptionValues().get(optionAvoidTerminal);
		if (avoidTerminal != null && avoidTerminal)
		{
			states = stateModel.getNonTerminalStateValues();
		}
		else
		{
			states = stateModel.getStates().stream().map(State::getValue).collect(Collectors.toList());
		}
		String distribution = (String) getOptionValues().get(optionDistribution);
		String result = null;
		if ("ascending".equals(distribution))
		{
			int idx = choose(states.size());
			result = states.get(idx);
		}
		else if ("descending".equals(distribution))
		{
			int idx = choose(states.size());
			result = states.get((states.size() - 1) - idx);
		}
		else
		{
			result = ConjuringUtils.randomString(states.toArray(new String[]{}));
		}
		return result;
	}

	public String getDescription()
	{
		return "Picks a random state from the type's state model. " + "\n\n"
				+ "Distribution can be:\n\n - 'Even' (all states are equally likely to be chosen)\n"
				+ " - 'Descending' (the first state is most likely to be chosen and the last state least likely), or\n"
				+ " - 'Ascending' (the first state is least likely to be chosen and the last state most likely)";
	}
}
