package com.kelimebulmacasi.a1

import java.util.Locale

/**
 * Local, offline, user-reviewable English definitions. 30 distinct pairs per
 * CEFR level; no machine translation masquerading as an English definition.
 * Imported personal definitions live in Wq186MatchingFolders, never FSRS.
 */
internal object Wq186MeaningBank {
    private val entries = """
A1|apple|a round fruit that may be red, green or yellow
A1|book|pages with words or pictures held together
A1|chair|a seat for one person, often with a back
A1|window|an opening with glass that lets light into a room
A1|teacher|a person whose job is helping students learn
A1|student|a person who studies at a school or college
A1|family|people related to one another, such as parents and children
A1|school|a place where children and adults go to learn
A1|garden|a piece of land where flowers or vegetables grow
A1|kitchen|the room where people prepare food
A1|water|a clear liquid that people and animals need to live
A1|bread|a baked food made mainly from flour and water
A1|morning|the early part of the day before noon
A1|evening|the part of the day between afternoon and night
A1|friend|someone you know well and like
A1|happy|feeling pleasure or joy
A1|small|not large in size
A1|beautiful|pleasant to look at
A1|sleep|to rest with your eyes closed and your mind less active
A1|walk|to move forward by taking steps on your feet
A1|listen|to pay attention to a sound
A1|speak|to say words aloud
A1|write|to form letters or words on a surface
A1|read|to look at written words and understand them
A1|country|an area of land with its own government
A1|city|a large town with many people and buildings
A1|market|a place where people buy and sell goods
A1|hospital|a place where sick people receive medical care
A1|doctor|a person trained to treat illness and injury
A1|animal|a living creature that is not a plant
A1|flower|the colourful part of a plant that can make seeds
A1|river|a large natural stream of water
A1|weather|the condition of the air outside at a certain time
A1|music|sounds organised into songs or other pieces
A1|question|a sentence used to ask for information
A2|journey|the act of travelling from one place to another
A2|neighbour|a person who lives near your home
A2|appointment|a time arranged to meet someone
A2|decision|a choice made after thinking about options
A2|holiday|time away from work or school for rest or travel
A2|bicycle|a vehicle with two wheels that you move with pedals
A2|message|information sent to another person by text or speech
A2|calendar|a chart showing days weeks and months
A2|exercise|physical activity done to keep healthy
A2|healthy|being in good physical or mental condition
A2|opinion|what someone thinks or believes about something
A2|explain|to make an idea clear by giving information
A2|borrow|to take something for a time and return it
A2|lend|to give something temporarily expecting it back
A2|prepare|to get something ready for use or an event
A2|discover|to find something for the first time
A2|repair|to make something broken work again
A2|compare|to look at two things to find similarities or differences
A2|customer|someone who buys goods or services
A2|restaurant|a place where meals are prepared and sold
A2|adventure|an unusual or exciting experience
A2|direction|the way that a place or object is pointing
A2|receipt|a paper or digital record of a payment
A2|opportunity|a chance to do something useful or desirable
A2|environment|the natural world and conditions around living things
A2|comfortable|making someone feel physically relaxed
A2|important|having a lot of value or meaning
A2|improve|to become better or make something better
A2|surprise|something unexpected that causes a strong reaction
A2|experience|knowledge gained from doing or living through things
A2|invite|to ask someone to come to a place or event
A2|collect|to bring things together into one place
A2|return|to go or come back to a previous place
B1|achievement|something successfully completed through effort
B1|advantage|a condition that makes success more likely
B1|attitude|the way someone thinks or feels about something
B1|balance|a state in which different parts have suitable proportions
B1|behaviour|the way a person or animal acts
B1|challenge|a difficult task that tests ability
B1|community|a group of people living or working together
B1|confident|feeling sure about your own abilities
B1|consequence|a result produced by an action or event
B1|contribute|to give something that helps achieve a shared goal
B1|decrease|to become smaller in number amount or size
B1|demand|a strong need or request for something
B1|development|the process of growing or becoming more advanced
B1|efficient|working well without wasting time or resources
B1|encourage|to give someone confidence or support
B1|evidence|facts or information that support a claim
B1|flexible|able to change or adapt easily
B1|independent|not controlled by or dependent on someone else
B1|influence|the power to affect how someone thinks or acts
B1|maintain|to keep something in good condition or at the same level
B1|motivation|the reason or desire that makes someone act
B1|negotiate|to discuss something to reach an agreement
B1|obstacle|something that prevents progress or makes it harder
B1|perspective|a particular way of thinking about a situation
B1|priority|something considered more important than other things
B1|reliable|able to be trusted to work or behave well
B1|responsibility|a duty to take care of something
B1|solution|a way to solve a problem
B1|strategy|a plan for achieving a goal
B1|volunteer|someone who offers to help without being required
B2|adaptation|a change made to fit new conditions
B2|ambiguous|having more than one possible meaning
B2|anticipate|to expect something before it happens
B2|assumption|something accepted as true without proof
B2|capacity|the amount that something can hold or do
B2|coherent|clear and logically connected
B2|collaborate|to work with others toward a shared result
B2|compromise|an agreement in which each side gives up something
B2|constraint|a rule or condition that limits choices
B2|controversy|a public disagreement about a subject
B2|crucial|extremely important to the outcome
B2|deliberate|done consciously and with careful thought
B2|diversity|the presence of different types or backgrounds
B2|emerge|to become visible or known
B2|emphasise|to give special attention or importance to something
B2|evaluate|to judge value or quality after examination
B2|feasible|possible to do successfully with available resources
B2|fluctuate|to rise and fall irregularly
B2|fundamental|forming a necessary or basic part of something
B2|implement|to put a plan or decision into action
B2|implication|a possible effect or meaning of an action
B2|innovation|a new idea or method that brings improvement
B2|integrity|the quality of being honest and following strong principles
B2|justify|to give reasons showing that an action is reasonable
B2|perception|the way something is understood or seen
B2|persistent|continuing firmly despite difficulty
B2|relevant|closely connected to the matter being discussed
B2|sustainable|able to continue without using up resources
B2|transition|a change from one state or stage to another
B2|uncertainty|a lack of confidence about what will happen
C1|adversity|a difficult situation that causes hardship
C1|alleviate|to make suffering or a problem less severe
C1|articulate|able to express ideas clearly and effectively
C1|bias|an unfair preference that affects judgment
C1|catalyst|something that causes an important change
C1|comprehensive|covering all major aspects of a subject
C1|concession|something given up during a disagreement or negotiation
C1|consensus|general agreement among members of a group
C1|contradiction|a statement or situation that conflicts with another
C1|corroborate|to support a claim with additional evidence
C1|detrimental|causing harm or damage
C1|discrepancy|a difference between things expected to agree
C1|elaborate|to explain something with additional detail
C1|empirical|based on observation or experiment
C1|enduring|continuing for a long time
C1|exacerbate|to make a problem worse
C1|hypothesis|a proposed explanation that can be tested
C1|inadvertent|happening unintentionally
C1|inherent|existing as a natural or permanent quality
C1|meticulous|showing extreme care and attention to details
C1|nuance|a small but important difference in meaning
C1|paradox|a situation that seems contradictory but may be true
C1|plausible|seeming reasonable or likely to be true
C1|precedent|an earlier case used as a guide for a later one
C1|reconcile|to make different beliefs or facts consistent
C1|resilience|the ability to recover after difficulty
C1|scrutinise|to examine closely and carefully
C1|substantiate|to provide evidence proving that something is true
C1|tentative|not final and open to change
C1|viable|able to work or succeed in practice
C2|aberration|a departure from what is normal or expected
C2|acquiesce|to accept something reluctantly without protest
C2|anomalous|different from what is usual or expected
C2|apocryphal|of doubtful authenticity despite being widely repeated
C2|assiduous|showing constant effort and careful attention
C2|circumspect|careful to consider possible risks before acting
C2|conflagration|a very large destructive fire
C2|conundrum|a difficult and confusing problem
C2|deleterious|causing harm over time
C2|dichotomy|a division into two contrasting parts
C2|disingenuous|pretending to be sincere while hiding the truth
C2|equanimity|calmness in a difficult or stressful situation
C2|ephemeral|lasting for only a very short time
C2|esoteric|understood by only a small specialist group
C2|fastidious|very attentive to accuracy and details
C2|idiosyncratic|having unusual characteristics specific to one person
C2|ineffable|too great or unusual to be expressed in words
C2|intransigent|unwilling to change an opinion or position
C2|laconic|using very few words in speech or writing
C2|magnanimous|generous toward a rival or less powerful person
C2|obfuscate|to make something deliberately unclear
C2|ostensible|appearing to be true but possibly different in reality
C2|perfunctory|done with little interest or care
C2|perspicacious|quick to understand complex things
C2|quintessential|representing the most typical qualities of something
C2|recalcitrant|stubbornly resisting authority or control
C2|recondite|little known and difficult to understand
C2|sagacious|showing wise and sound judgment
C2|superfluous|unnecessary because more than enough already exists
C2|ubiquitous|present or found almost everywhere
    """.trimIndent().lineSequence().mapNotNull { row ->
        val parts = row.trim().split('|', limit = 3)
        if (parts.size != 3) null else
            Wq175MatchPair(parts[0], "English definition", parts[1], parts[2])
    }.toList()

    fun load(levels: Collection<String>): List<Wq175MatchPair> {
        val allowed = levels.toSet()
        return entries.filter { it.level in allowed }
            .distinctBy { it.english.lowercase(Locale.ROOT) }
            .distinctBy { it.turkish.lowercase(Locale.ROOT) }
    }

    fun hasEnough(levels: Collection<String>, requested: Int): Boolean =
        load(levels).size >= requested
}
