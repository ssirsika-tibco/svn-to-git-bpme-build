/*
* ENVIRONMENT:    Java Generic
*
* COPYRIGHT:      (C) 2016 TIBCO Software Inc
*/
package com.tibco.bpm.acecannon.casedata;

import java.util.Arrays;
import java.util.List;

/**
 * Generates weird objects using a number of configurable options
 * @author smorgan
 *
 */
public class BizarreObjectConjurer extends AbstractConjurer<String> implements ValueConjurer<String>
{
	private static String[]	NOUNS					= {"Apple", "Banana", "Cherry", "Date", "Elderberry", "Fig",
			"Grape", "Honeydew Melon", "Pig", "Cow", "Horse", "Donkey", "Zafira", "Accord", "Lamp", "Plant", "Monitor",
			"Telephone", "Photograph", "Can", "Cup", "Bowl", "Plate", "Mat", "Table", "Chair", "Carpet", "Desk",
			"Shoes", "Socks", "Pants", "Trousers", "T-Shirt", "Hat", "Undercrackers", "Guava", "Grapefruit",
			"Jackfruit", "Kumquat", "Kiwifruit", "Lemon", "Lime", "Raspberry Pi", "Laptop", "Tablet", "Coffee Machine",
			"Car", "Bicycle", "Motorbike", "Scooter", "Skateboard", "Coat", "Adrian", "Drink", "Shoes", "Jiffy Bag",
			"Pizza", "Christmas Cake", "Partition Wall", "Bag-for-Life", "Calendar", "Baguette", "Pasty",
			"Danish Pastry", "Board Game", "Handbrake", "Binder", "Painting", "Wiper", "Pizza", "Flapjack", "Building",
			"Clown", "Monkey", "Gibbon", "Pig", "Sheep", "Ocelot", "Ferret", "Badger", "Loyalty Card", "Angle Grinder",
			"Circular Saw", "Lawnmower", "Model Village", "Cookie", "Hog-rider", "Wizard", "Witch", "Giant", "Sparky",
			"Goblin", "Skeleton", "Elite Barbarian", "Fireball", "Leprechaun", "Steve", "Creeper", "Pigman", "Ghast",
			"Enderdragon", "Assets", "Stollen", "Headphones", "Microbit", "Python", "Fishtank", "Shrimp", "Kingdomino",
			"Goblin gang", "Skeleton army", "Yoghurt", "Alpaca", "Llama", "Camel", "Cat", "Dog", "Catnip", "Flotilla",
			"Rockpool", "Certificate", "Postcard", "Dishwasher", "Fridge", "Squeezebox", "Vauxhall", "Zafira", "Cannon",
			"Ragamuffin", "Bottle", "Glass", "Painting", "Sink", "Vacuum Cleaner", "Board Room", "Football Pitch",
			"Pavement", "Boot", "Holiday", "Mustard", "Chili Jam", "Coleslaw", "Meal", "Carrot", "Spot", "Picture",
			"Smell", "Kitchen", "Power Station", "Residence", "Shop", "Stench", "Partridge", "Monkey", "Tennis ball",
			"Whisper", "Rice", "Mackerel", "Windmill", "Hill", "Payload", "Cluster flip", "Smartphone", "Wallet",
			"Ticket", "Kettle", "Hamster", "Computer", "Games console", "Arcade machine", "Monkey's uncle",
			"\"Pop\" goes the weasel", "Squirrel", "Toaster", "Paella", "Chicken", "Prawn", "Chorizo", "Stollen",
			"Piano", "Naan bread", "Towel", "Thermometer", "Cannon", "Rifle", "Saber", "Sword", "Doughnut"};

	private static String[]	COLOURS					= {"Red", "Green", "Blue", "Orange", "Yellow", "Pink", "Black",
			"White", "Purple", "Orange", "Golden", "Silver", "Gold", "Copper", "Bronze", "Platinum", "Beige", "Brown",
			"Grey", "Turquoise", "Aqua", "Mauve", "Violet", "Magenta", "Indigo", "Hamlindigo Blue", "Olive", "Cyan",
			"Salmon pink", "Maroon", "Lime", "Teal", "Lavender", "Azure", "Plum", "Tan", "Goldenrod", "Crimson",
			"Khaki", "Lilac", "Azure", "Gunmetal", "Scarlet", "Blush", "Burgundy", "Byzantium", "Cerulean", "Amaranth",
			"Amber", "Aquamarine", "Baby blue", "Salmon pink", "Natural", "Cobalt", "Ochre", "Ruby", "Fawn", "Cream",
			"Rose", "Periwinkle", "Raspberry", "Slate", "Viridian"};

	private static String[]	ADJECTIVES				= {"Enormous", "Interesting", "Mouldy", "Smelly", "Gigantic",
			"Absurd", "Tasty", "Sad", "Massive", "Tiny", "Fashionable", "Swollen", "Bulging", "Shrunken", "Dehydrated",
			"Explosive", "Unpopular", "Fantastic", "Tragic", "Hyperactive", "Stinky", "Pungent", "Hot", "Warm", "Cold",
			"Freezing", "Energetic", "Lazy", "Lively", "Slow", "Fast", "Tired", "Nullified", "Stinking", "Wet", "Moist",
			"Damp", "Soggy", "Rampant", "Volatile", "Excited", "Sleepy", "Drowsy", "Over-achieving", "Disappointed",
			"Regretful", "Sticky", "Adhesive", "Crumpled", "Confused", "Throbbing", "Withdrawn", "Stale", "Complicated",
			"Competitive", "Compromised", "Embarrassed", "Miniature", "Charged", "Bouncey", "Wobbly", "Reliable",
			"Deflated", "Simple", "Complicated", "Attractive", "Ugly", "Perfect", "Ideal", "Unreliable", "Plump",
			"Small", "Doubtful", "Antagonised", "Inflated", "Deflated", "Curious", "Threatening", "Thought-provoking",
			"Dull", "Boring", "Non-specific", "Ambiguous", "Pressurised", "Wearable", "Disposable", "Reusable",
			"Mind-numbing", "Tedious", "Rotten", "Musty", "Rancid", "Putrid", "Grotty", "Ghastly", "Awful", "Tidy",
			"Braggadocious", "Questionable", "Doubtful", "Confident", "Shy", "Firm", "Substantial", "Unnecessary",
			"Redundant", "Truly impressive", "Supposedly unique", "Incompetent", "Farcical", "Badly-organised",
			"Shambolic", "Mind-boggling", "World's greatest", "Stolen", "Bombastic", "Overinflated", "Excitable",
			"Twitchy", "Temperamental", "Schrödinger's", "Crazy"};

	private static String	NORMAL					= "abcdefghijklmnopqrstuvwxyz_,;.?!/\\'"
			+ "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789";

	private static String	UPSIDE_DOWN				= "ɐqɔpǝɟbɥıظʞןɯuodbɹsʇnʌʍxʎz‾'؛˙¿¡/\\,"
			+ "∀qϽᗡƎℲƃHIſʞ˥WNOԀὉᴚS⊥∩ΛMXʎZ" + "0ƖᄅƐㄣϛ9ㄥ86";

	private static String	A_ACCENT				= "ÁÂÃÄÅ";

	private static String	E_ACCENT				= "ÉÈÊË";

	private static String	I_ACCENT				= "ÍÌÎÏ";

	private static String	O_ACCENT				= "ÓÒÕÖ";

	private static String	U_ACCENT				= "ÚÙÛÜ";

	private static String	AL_ACCENT				= "áàâãäå";

	private static String	EL_ACCENT				= "éèêë";

	private static String	IL_ACCENT				= "íìîï";

	private static String	OL_ACCENT				= "óòôõö";

	private static String	UL_ACCENT				= "úùûü";

	public static Option	optionAdjective			= new Option(OptionType.BOOLEAN, "includeAdjective",
			"Include adjective");

	public static Option	optionColour			= new Option(OptionType.BOOLEAN, "includeColour", "Include colour");

	public static Option	optionAccents			= new Option(OptionType.BOOLEAN, "addAccents", "Add accents");

	public static Option	optionUpsideDown		= new Option(OptionType.BOOLEAN, "turnUpsideDown",
			"Turn text upside-down");

	public static Option	optionScriptInjection	= new Option(OptionType.BOOLEAN, "scriptInjection",
			"Script injection");

	@Override
	public List<Option> getOptions()
	{
		return Arrays.asList(optionAdjective, optionColour, optionUpsideDown, optionAccents, optionScriptInjection);
	}

	private String applyAccents(String s)
	{
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			char r = 0;
			switch (c)
			{
				case 'A':
					r = ConjuringUtils.randomChar(A_ACCENT);
					break;
				case 'E':
					r = ConjuringUtils.randomChar(E_ACCENT);
					break;
				case 'I':
					r = ConjuringUtils.randomChar(I_ACCENT);
					break;
				case 'O':
					r = ConjuringUtils.randomChar(O_ACCENT);
					break;
				case 'U':
					r = ConjuringUtils.randomChar(U_ACCENT);
					break;
				case 'a':
					r = ConjuringUtils.randomChar(AL_ACCENT);
					break;
				case 'e':
					r = ConjuringUtils.randomChar(EL_ACCENT);
					break;
				case 'i':
					r = ConjuringUtils.randomChar(IL_ACCENT);
					break;
				case 'o':
					r = ConjuringUtils.randomChar(OL_ACCENT);
					break;
				case 'u':
					r = ConjuringUtils.randomChar(UL_ACCENT);
					break;
			}
			buf.append(r == 0 ? c : r);
		}
		return buf.toString();
	}

	private String turnUpsideDown(String s)
	{
		StringBuilder buf = new StringBuilder();
		for (int i = s.length() - 1; i >= 0; i--)
		{
			char c = s.charAt(i);
			int idx = NORMAL.indexOf(c);
			if (idx >= 0)
			{
				buf.append(UPSIDE_DOWN.charAt(idx));
			}
			else
			{
				buf.append(c);
			}
		}
		return buf.toString();
	}

	@Override
	public String conjure()
	{
		StringBuilder buf = new StringBuilder();
		Boolean useAdjective = (Boolean) getOptionValues().get(optionAdjective);
		if (useAdjective != null && useAdjective)
		{
			buf.append(ConjuringUtils.randomString(ADJECTIVES));
			buf.append(" ");
		}
		Boolean useColour = (Boolean) getOptionValues().get(optionColour);
		if (useColour != null && useColour)
		{
			buf.append(ConjuringUtils.randomString(COLOURS));
			buf.append(" ");
		}
		buf.append(ConjuringUtils.randomString(NOUNS));
		String s = buf.toString().toLowerCase();
		s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
		Boolean upsideDown = (Boolean) getOptionValues().get(optionUpsideDown);
		if (upsideDown != null && upsideDown)
		{
			s = turnUpsideDown(s);
		}
		Boolean accents = (Boolean) getOptionValues().get(optionAccents);
		if (accents != null && accents)
		{
			s = applyAccents(s);
		}
		Boolean scriptInjection = (Boolean) getOptionValues().get(optionScriptInjection);
		if (scriptInjection != null && scriptInjection)
		{
			s = String.format("<script>alert(\"HACK ATTACK by %s!\");</script>", s);
		}
		return s;
	}

	@Override
	public String getDescription()
	{
		return "Generates weird objects using a shedload of configurable options";
	}
}
