package com.scholr.app.data

import com.scholr.app.data.model.Conference
import com.scholr.app.data.model.Interest
import com.scholr.app.data.model.Paper
import com.scholr.app.data.model.PaperCollection

/**
 * In-memory content so the whole journey is walkable without a backend.
 * Swap this object for a repository when you wire up a real API — every
 * screen already reads through [com.scholr.app.ScholrViewModel].
 */
object SampleData {

    val interests = listOf(
        Interest("cs", "Computer Science", "💻"),
        Interest("neuro", "Neuroscience", "🧠"),
        Interest("bio", "Biology", "🧬"),
        Interest("phys", "Physics", "⚛️"),
        Interest("chem", "Chemistry", "⚗️"),
        Interest("math", "Mathematics", "📐"),
        Interest("med", "Medicine", "🩺"),
        Interest("psy", "Psychology", "🫀"),
        Interest("climate", "Climate Science", "🌍"),
        Interest("econ", "Economics", "📈"),
        Interest("ml", "Machine Learning", "🤖"),
        Interest("robotics", "Robotics", "🦾")
    )

    val quickFilters = listOf(
        "For you", "Trending", "New this week", "Highly cited", "Open access", "Preprints"
    )

    val papers = listOf(
        Paper(
            id = "p1",
            title = "Sparse Attention Routing for Long-Context Scientific Retrieval",
            authors = "L. Marchetti, D. Okonkwo, S. Ravindran",
            venue = "NeurIPS",
            year = 2025,
            field = "Computer Science",
            citations = 412,
            readMinutes = 14,
            abstractText = "Retrieval over book-length scientific corpora remains bottlenecked by " +
                "quadratic attention. We introduce a learned routing layer that selects a sparse " +
                "subset of key blocks per query token, cutting attention FLOPs by 71% while " +
                "improving nDCG@10 on four scholarly benchmarks. The router is trained with a " +
                "differentiable top-k relaxation and requires no changes to the base encoder.",
            keyFindings = listOf(
                "71% reduction in attention FLOPs at equal retrieval quality",
                "Outperforms dense baselines on 4 of 5 scholarly benchmarks",
                "Router transfers zero-shot across three encoder families"
            ),
            tags = listOf("Retrieval", "Efficiency", "Transformers")
        ),
        Paper(
            id = "p2",
            title = "Hippocampal Replay Predicts Consolidation of Abstract Rules",
            authors = "A. Bergström, N. Fadel, Y. Tanaka, R. Iyer",
            venue = "Nature Neuroscience",
            year = 2025,
            field = "Neuroscience",
            citations = 268,
            readMinutes = 18,
            abstractText = "Whether sharp-wave ripple replay supports abstraction, rather than " +
                "verbatim episodic recall, is unresolved. Recording from 34 participants with " +
                "intracranial electrodes during a rule-learning task, we show replay content " +
                "systematically drifts from exemplar-specific to rule-general across sleep, and " +
                "that the magnitude of this drift predicts next-day transfer performance.",
            keyFindings = listOf(
                "Replay content abstracts across a single night of sleep",
                "Drift magnitude predicts transfer accuracy (r = 0.63)",
                "Effect is absent in a sleep-deprived control group"
            ),
            tags = listOf("Memory", "Sleep", "Electrophysiology")
        ),
        Paper(
            id = "p3",
            title = "A Programmable Enzyme Cascade for Ambient Ammonia Synthesis",
            authors = "M. Duarte, H. Rehman, C. Villalobos",
            venue = "Science",
            year = 2026,
            field = "Chemistry",
            citations = 96,
            readMinutes = 11,
            abstractText = "Haber–Bosch accounts for roughly 1.4% of global CO2 emissions. We " +
                "report a three-enzyme cascade immobilised on a conductive hydrogel that fixes " +
                "dinitrogen at ambient temperature and pressure with a faradaic efficiency of " +
                "38%, sustained over 200 hours of continuous operation.",
            keyFindings = listOf(
                "38% faradaic efficiency at ambient conditions",
                "200 h continuous operation with <9% activity loss",
                "Cascade composition is tunable by plasmid swap"
            ),
            tags = listOf("Catalysis", "Green Chemistry", "Enzymes")
        ),
        Paper(
            id = "p4",
            title = "Revisiting Scaling Laws Under Data Constraint",
            authors = "P. Nkemelu, J. Sato, E. Lindqvist",
            venue = "ICML",
            year = 2025,
            field = "Computer Science",
            citations = 1_143,
            readMinutes = 21,
            abstractText = "Existing compute-optimal scaling prescriptions assume effectively " +
                "unlimited unique tokens. We derive and empirically validate a corrected frontier " +
                "for the repeated-data regime, showing that the optimal parameter count falls " +
                "sublinearly once the unique-token budget is fixed, and that four epochs remains " +
                "close to lossless while sixteen is decidedly not.",
            keyFindings = listOf(
                "Optimal N falls sublinearly under a fixed token budget",
                "Up to 4 epochs of repetition is near-lossless",
                "Frontier validated across 3 orders of magnitude of compute"
            ),
            tags = listOf("Scaling", "Training", "Empirical")
        ),
        Paper(
            id = "p5",
            title = "Ocean Heat Uptake Explains the Post-2020 Warming Acceleration",
            authors = "K. Abebe, T. Lindgren, W. Choi",
            venue = "Nature Climate Change",
            year = 2026,
            field = "Climate Science",
            citations = 187,
            readMinutes = 16,
            abstractText = "The observed 2020–2025 acceleration in global mean surface temperature " +
                "exceeds CMIP6 ensemble expectations. Combining Argo profiles with satellite " +
                "radiative-imbalance estimates, we attribute 0.09 °C/decade of the excess to a " +
                "reduction in shortwave-reflecting aerosols over shipping lanes.",
            keyFindings = listOf(
                "0.09 °C/decade attributable to aerosol reduction",
                "Argo and CERES estimates agree within uncertainty",
                "Implies a near-term overshoot of the 1.5 °C threshold"
            ),
            tags = listOf("Attribution", "Aerosols", "Ocean")
        ),
        Paper(
            id = "p6",
            title = "Topological Protection in Twisted Trilayer Graphene at 1.6 K",
            authors = "S. Kaur, F. Moreau, B. Ivanov",
            venue = "Physical Review Letters",
            year = 2025,
            field = "Physics",
            citations = 331,
            readMinutes = 13,
            abstractText = "We report transport signatures of a topologically protected edge mode " +
                "in magic-angle twisted trilayer graphene persisting to 1.6 K, an order of " +
                "magnitude above prior bilayer results, and show the gap scales with " +
                "displacement field as predicted by a continuum model.",
            keyFindings = listOf(
                "Edge mode survives to 1.6 K in trilayer stacks",
                "Gap scales linearly with displacement field",
                "Continuum model reproduces the phase boundary"
            ),
            tags = listOf("Condensed Matter", "2D Materials", "Topology")
        ),
        Paper(
            id = "p7",
            title = "Causal Inference From Wearables Without Randomisation",
            authors = "R. Delgado, M. Haddad, A. Petrov",
            venue = "JAMA Network Open",
            year = 2026,
            field = "Medicine",
            citations = 74,
            readMinutes = 12,
            abstractText = "Continuous wearable streams offer dense within-person variation that " +
                "can substitute for randomisation under testable assumptions. We formalise a " +
                "negative-control design for wearable time series and apply it to 41,000 " +
                "participants, recovering three of four known drug effects within 12% of the " +
                "trial estimates.",
            keyFindings = listOf(
                "Recovers 3 of 4 known effects within 12% of RCT estimates",
                "Negative controls detect the one failure case",
                "Method requires no external control cohort"
            ),
            tags = listOf("Causal Inference", "Digital Health", "Wearables")
        ),
        Paper(
            id = "p8",
            title = "Metacognitive Confidence Is Domain-General After All",
            authors = "I. Novak, G. Mbeki, L. Ferreira",
            venue = "Psychological Science",
            year = 2025,
            field = "Psychology",
            citations = 152,
            readMinutes = 10,
            abstractText = "Reports of domain-specific metacognition may reflect task-difficulty " +
                "confounds rather than genuine dissociation. Using difficulty-matched perceptual " +
                "and memory tasks in 890 participants, we find a single latent confidence factor " +
                "explains 74% of variance across domains.",
            keyFindings = listOf(
                "One latent factor explains 74% of cross-domain variance",
                "Prior dissociations vanish after difficulty matching",
                "Replicated in a pre-registered second sample"
            ),
            tags = listOf("Metacognition", "Replication", "Psychophysics")
        )
    )

    val conferences = listOf(
        Conference(
            id = "c1", acronym = "NeurIPS", name = "Neural Information Processing Systems",
            location = "Vancouver, Canada", dates = "Dec 8 – Dec 14, 2026",
            deadline = "Abstract due May 15", daysLeft = 12,
            field = "Computer Science", tier = "A*"
        ),
        Conference(
            id = "c2", acronym = "SfN", name = "Society for Neuroscience Annual Meeting",
            location = "Chicago, USA", dates = "Oct 17 – Oct 21, 2026",
            deadline = "Abstract due Apr 30", daysLeft = 4,
            field = "Neuroscience", tier = "A"
        ),
        Conference(
            id = "c3", acronym = "ICML", name = "International Conference on Machine Learning",
            location = "Seoul, South Korea", dates = "Jul 12 – Jul 18, 2026",
            deadline = "Full paper due Jan 28", daysLeft = 31,
            field = "Computer Science", tier = "A*"
        ),
        Conference(
            id = "c4", acronym = "ACS Spring", name = "American Chemical Society Spring Meeting",
            location = "Boston, USA", dates = "Mar 22 – Mar 26, 2026",
            deadline = "Abstract due Nov 3", daysLeft = 58,
            field = "Chemistry", tier = "A"
        ),
        Conference(
            id = "c5", acronym = "AGU", name = "American Geophysical Union Fall Meeting",
            location = "New Orleans, USA", dates = "Dec 14 – Dec 18, 2026",
            deadline = "Abstract due Aug 6", daysLeft = 21,
            field = "Climate Science", tier = "A"
        ),
        Conference(
            id = "c6", acronym = "APS March", name = "APS March Meeting",
            location = "Denver, USA", dates = "Mar 2 – Mar 6, 2026",
            deadline = "Abstract due Oct 24", daysLeft = 9,
            field = "Physics", tier = "A"
        )
    )

    val collections = listOf(
        PaperCollection("col1", "Thesis — Chapter 3", listOf("p1", "p4"), "📘"),
        PaperCollection("col2", "Reading group", listOf("p2", "p8"), "☕"),
        PaperCollection("col3", "Methods to steal", listOf("p7", "p3", "p5"), "🧪")
    )

    val recentSearches = listOf(
        "sparse attention retrieval",
        "hippocampal replay abstraction",
        "ambient ammonia synthesis",
        "scaling laws repeated data"
    )

    val trendingTopics = listOf(
        "Mechanistic interpretability",
        "Enzymatic nitrogen fixation",
        "Wearable causal inference",
        "Twisted multilayer graphene",
        "Aerosol forcing"
    )

    fun paperById(id: String): Paper? = papers.firstOrNull { it.id == id }
}
