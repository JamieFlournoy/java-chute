Feature: Virus Scanner Example


  Scenario: Single-Thread Virus Scanner Example
    Given I am running the Single-Threaded Virus Scanner Example
    When I run the program
    Then I should see the output
    """
    chapter_0.txt contains the "ahoy" virus!
    chapter_1_loomings.txt contains the "yonder" virus!
    chapter_3_the_spouter_inn.txt contains the "tallow" virus!
    chapter_6_the_street.txt contains the "yonder" virus!
    chapter_9_the_sermon.txt contains the "gangway" virus!
    chapter_17_the_ramadan.txt contains the "yonder" virus!
    chapter_18_his_mark.txt contains the "gangway" virus!
    chapter_22_merry_christmas.txt contains the "ahoy" virus!
    chapter_32_cetology.txt contains the "tallow" virus!
    chapter_36_the_quarter_deck.txt contains the "gangway" and "yonder" viruses!
    chapter_37_sunset.txt contains the "yonder" virus!
    chapter_40_midnight_forecastle.txt contains the "yonder" virus!
    chapter_48_the_first_lowering.txt contains the "ahoy" and "yonder" viruses!
    chapter_52_the_albatross.txt contains the "ahoy" virus!
    chapter_54_the_town_ho_s_story.txt contains the "yonder" virus!
    chapter_89_fast_fish_and_loose_fish.txt contains the "yonder" virus!
    chapter_91_the_pequod_meets_the_rose_bud.txt contains the "ahoy" and "tallow" viruses!
    chapter_100_leg_and_arm.txt contains the "ahoy" virus!
    chapter_102_a_bower_in_the_arsacides.txt contains the "tallow" virus!
    chapter_109_ahab_and_starbuck_in_the_cabin.txt contains the "gangway" virus!
    chapter_113_the_forge.txt contains the "ahoy" virus!
    chapter_119_the_candles.txt contains the "yonder" virus!
    chapter_127_the_deck.txt contains the "gangway" virus!
    chapter_131_the_pequod_meets_the_delight.txt contains the "yonder" virus!
    chapter_132_the_symphony.txt contains the "yonder" virus!
    chapter_134_the_chase_second_day.txt contains the "yonder" virus!

    """

  Scenario: Single-Thread Virus Scanner Example, Verbose Output
    Given I am running the Single-Threaded Virus Scanner Example with Verbose Output
    When I run the program
    Then I should see the output
    """
	Scanning for viruses...
    chapter_0.txt contains the "ahoy" virus!
      line 748:   ahoy! Do you see that whale now?” “Ay ay, sir!  A shoal of Sperm
    chapter_1_loomings.txt contains the "yonder" virus!
      line 39: extremest limit of the land; loitering under the shady lee of yonder
      line 63: from yonder cottage goes a sleepy smoke. Deep into distant woodlands
    chapter_3_the_spouter_inn.txt contains the "tallow" virus!
      line 110: he couldn’t afford it. Nothing but two dismal tallow candles, each in a
    chapter_6_the_street.txt contains the "yonder" virus!
      line 56: Go and gaze upon the iron emblematical harpoons round yonder lofty
    chapter_9_the_sermon.txt contains the "gangway" virus!
      line 3: the scattered people to condense. “Starboard gangway, there! side away
      line 4: to larboard—larboard gangway to starboard! Midships! midships!”
    chapter_17_the_ramadan.txt contains the "yonder" virus!
      line 36: thought I; but at any rate, since the harpoon stands yonder, and he
    chapter_18_his_mark.txt contains the "gangway" virus!
      line 80: gangway. “Quick, I say, you Bildad, and get the ship’s papers. We must
    chapter_22_merry_christmas.txt contains the "ahoy" virus!
      line 138: there! Boat ahoy! Stand by to come close alongside, now! Careful,
    chapter_32_cetology.txt contains the "tallow" virus!
      line 348: quite alone by themselves, burn unsavory tallow instead of odorous wax.
    chapter_36_the_quarter_deck.txt contains the "gangway" and "yonder" viruses!
      line 6: cabin-gangway to the deck. There most sea-captains usually walk at that
      line 200: indignity. I meant not to incense thee. Let it go. Look! see yonder
      line 205: matter of the whale? See Stubb! he laughs! See yonder Chilian! he
    chapter_37_sunset.txt contains the "yonder" virus!
      line 8: Yonder, by ever-brimming goblet’s rim, the warm waves blush like wine.
    chapter_40_midnight_forecastle.txt contains the "yonder" virus!
      line 150: sea. Our captain has his birthmark; look yonder, boys, there’s another
    chapter_48_the_first_lowering.txt contains the "ahoy" and "yonder" viruses!
      line 103: “Mr. Starbuck! larboard boat there, ahoy! a word with ye, sir, if ye
      line 54: “Never heed yonder yellow boys, Archy.”
      line 63: my boys? What is it you stare at? Those chaps in yonder boat? Tut! They
    chapter_52_the_albatross.txt contains the "ahoy" virus!
      line 24: “Ship ahoy! Have ye seen the White Whale?”
      line 37: and shortly bound home, he loudly hailed—“Ahoy there! This is the
    chapter_54_the_town_ho_s_story.txt contains the "yonder" virus!
      line 746: soon as Steelkilt leaves me, I swear to beach this boat on yonder
    chapter_89_fast_fish_and_loose_fish.txt contains the "yonder" virus!
      line 112: last mite but a Fast-Fish? What is yonder undetected villain’s marble
    chapter_91_the_pequod_meets_the_rose_bud.txt contains the "ahoy" and "tallow" viruses!
      line 92: bawled—“Bouton-de-Rose, ahoy! are there any of you Bouton-de-Roses that
      line 51: tallow candles, and cases of snuffers, foreseeing that all the oil they
    chapter_100_leg_and_arm.txt contains the "ahoy" virus!
      line 4: “Ship, ahoy! Hast seen the White Whale?”
    chapter_102_a_bower_in_the_arsacides.txt contains the "tallow" virus!
      line 21: leviathan; and belike of the tallow-vats, dairy-rooms, butteries, and
    chapter_109_ahab_and_starbuck_in_the_cabin.txt contains the "gangway" virus!
      line 23: his back to the gangway door, was wrinkling his brow, and tracing his
    chapter_113_the_forge.txt contains the "ahoy" virus!
      line 108: “No, no—no water for that; I want it of the true death-temper. Ahoy,
    chapter_119_the_candles.txt contains the "yonder" virus!
      line 77: into a fair wind that will drive us towards home. Yonder, to windward,
    chapter_127_the_deck.txt contains the "gangway" virus!
      line 5: his frock.—Ahab comes slowly from the cabin-gangway, and hears Pip
    chapter_131_the_pequod_meets_the_delight.txt contains the "yonder" virus!
      line 49: “Ha! yonder! look yonder, men!” cried a foreboding voice in her wake.
    chapter_132_the_symphony.txt contains the "yonder" virus!
      line 140: in this world, like yonder windlass, and Fate is the handspike. And all
    chapter_134_the_chase_second_day.txt contains the "yonder" virus!
      line 239: inaccessible being. Can any lead touch yonder floor, any mast scrape
      line 240: yonder roof?—Aloft there! which way?”
    
    """

  Scenario: Parallel Virus Scanner Example
    Given I am running the Parallel Virus Scanner Example
    When I run the program
    Then I should see the output
    """
    chapter_0.txt contains the "ahoy" virus!
    chapter_1_loomings.txt contains the "yonder" virus!
    chapter_3_the_spouter_inn.txt contains the "tallow" virus!
    chapter_6_the_street.txt contains the "yonder" virus!
    chapter_9_the_sermon.txt contains the "gangway" virus!
    chapter_17_the_ramadan.txt contains the "yonder" virus!
    chapter_18_his_mark.txt contains the "gangway" virus!
    chapter_22_merry_christmas.txt contains the "ahoy" virus!
    chapter_32_cetology.txt contains the "tallow" virus!
    chapter_36_the_quarter_deck.txt contains the "gangway" and "yonder" viruses!
    chapter_37_sunset.txt contains the "yonder" virus!
    chapter_40_midnight_forecastle.txt contains the "yonder" virus!
    chapter_48_the_first_lowering.txt contains the "ahoy" and "yonder" viruses!
    chapter_52_the_albatross.txt contains the "ahoy" virus!
    chapter_54_the_town_ho_s_story.txt contains the "yonder" virus!
    chapter_89_fast_fish_and_loose_fish.txt contains the "yonder" virus!
    chapter_91_the_pequod_meets_the_rose_bud.txt contains the "ahoy" and "tallow" viruses!
    chapter_100_leg_and_arm.txt contains the "ahoy" virus!
    chapter_102_a_bower_in_the_arsacides.txt contains the "tallow" virus!
    chapter_109_ahab_and_starbuck_in_the_cabin.txt contains the "gangway" virus!
    chapter_113_the_forge.txt contains the "ahoy" virus!
    chapter_119_the_candles.txt contains the "yonder" virus!
    chapter_127_the_deck.txt contains the "gangway" virus!
    chapter_131_the_pequod_meets_the_delight.txt contains the "yonder" virus!
    chapter_132_the_symphony.txt contains the "yonder" virus!
    chapter_134_the_chase_second_day.txt contains the "yonder" virus!

    """

  Scenario: Parallel Virus Scanner Example, Verbose Output
    Given I am running the Parallel Virus Scanner Example with Verbose Output
    When I run the program
    Then I should see the output
    """
	Scanning for viruses...
    chapter_0.txt contains the "ahoy" virus!
      line 748:   ahoy! Do you see that whale now?” “Ay ay, sir!  A shoal of Sperm
    chapter_1_loomings.txt contains the "yonder" virus!
      line 39: extremest limit of the land; loitering under the shady lee of yonder
      line 63: from yonder cottage goes a sleepy smoke. Deep into distant woodlands
    chapter_3_the_spouter_inn.txt contains the "tallow" virus!
      line 110: he couldn’t afford it. Nothing but two dismal tallow candles, each in a
    chapter_6_the_street.txt contains the "yonder" virus!
      line 56: Go and gaze upon the iron emblematical harpoons round yonder lofty
    chapter_9_the_sermon.txt contains the "gangway" virus!
      line 3: the scattered people to condense. “Starboard gangway, there! side away
      line 4: to larboard—larboard gangway to starboard! Midships! midships!”
    chapter_17_the_ramadan.txt contains the "yonder" virus!
      line 36: thought I; but at any rate, since the harpoon stands yonder, and he
    chapter_18_his_mark.txt contains the "gangway" virus!
      line 80: gangway. “Quick, I say, you Bildad, and get the ship’s papers. We must
    chapter_22_merry_christmas.txt contains the "ahoy" virus!
      line 138: there! Boat ahoy! Stand by to come close alongside, now! Careful,
    chapter_32_cetology.txt contains the "tallow" virus!
      line 348: quite alone by themselves, burn unsavory tallow instead of odorous wax.
    chapter_36_the_quarter_deck.txt contains the "gangway" and "yonder" viruses!
      line 6: cabin-gangway to the deck. There most sea-captains usually walk at that
      line 200: indignity. I meant not to incense thee. Let it go. Look! see yonder
      line 205: matter of the whale? See Stubb! he laughs! See yonder Chilian! he
    chapter_37_sunset.txt contains the "yonder" virus!
      line 8: Yonder, by ever-brimming goblet’s rim, the warm waves blush like wine.
    chapter_40_midnight_forecastle.txt contains the "yonder" virus!
      line 150: sea. Our captain has his birthmark; look yonder, boys, there’s another
    chapter_48_the_first_lowering.txt contains the "ahoy" and "yonder" viruses!
      line 103: “Mr. Starbuck! larboard boat there, ahoy! a word with ye, sir, if ye
      line 54: “Never heed yonder yellow boys, Archy.”
      line 63: my boys? What is it you stare at? Those chaps in yonder boat? Tut! They
    chapter_52_the_albatross.txt contains the "ahoy" virus!
      line 24: “Ship ahoy! Have ye seen the White Whale?”
      line 37: and shortly bound home, he loudly hailed—“Ahoy there! This is the
    chapter_54_the_town_ho_s_story.txt contains the "yonder" virus!
      line 746: soon as Steelkilt leaves me, I swear to beach this boat on yonder
    chapter_89_fast_fish_and_loose_fish.txt contains the "yonder" virus!
      line 112: last mite but a Fast-Fish? What is yonder undetected villain’s marble
    chapter_91_the_pequod_meets_the_rose_bud.txt contains the "ahoy" and "tallow" viruses!
      line 92: bawled—“Bouton-de-Rose, ahoy! are there any of you Bouton-de-Roses that
      line 51: tallow candles, and cases of snuffers, foreseeing that all the oil they
    chapter_100_leg_and_arm.txt contains the "ahoy" virus!
      line 4: “Ship, ahoy! Hast seen the White Whale?”
    chapter_102_a_bower_in_the_arsacides.txt contains the "tallow" virus!
      line 21: leviathan; and belike of the tallow-vats, dairy-rooms, butteries, and
    chapter_109_ahab_and_starbuck_in_the_cabin.txt contains the "gangway" virus!
      line 23: his back to the gangway door, was wrinkling his brow, and tracing his
    chapter_113_the_forge.txt contains the "ahoy" virus!
      line 108: “No, no—no water for that; I want it of the true death-temper. Ahoy,
    chapter_119_the_candles.txt contains the "yonder" virus!
      line 77: into a fair wind that will drive us towards home. Yonder, to windward,
    chapter_127_the_deck.txt contains the "gangway" virus!
      line 5: his frock.—Ahab comes slowly from the cabin-gangway, and hears Pip
    chapter_131_the_pequod_meets_the_delight.txt contains the "yonder" virus!
      line 49: “Ha! yonder! look yonder, men!” cried a foreboding voice in her wake.
    chapter_132_the_symphony.txt contains the "yonder" virus!
      line 140: in this world, like yonder windlass, and Fate is the handspike. And all
    chapter_134_the_chase_second_day.txt contains the "yonder" virus!
      line 239: inaccessible being. Can any lead touch yonder floor, any mast scrape
      line 240: yonder roof?—Aloft there! which way?”

    """
