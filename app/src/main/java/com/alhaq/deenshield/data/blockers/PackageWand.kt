package com.alhaq.deenshield.data.blockers

class PackageWand {
    companion object {
        // Gaming Apps (Comprehensive list)
        val GAMING_APPS = hashSetOf(
            // Battle Royale & Shooters
            "com.pubg.imobile", "com.pubg.krmobile", "com.tencent.ig",
            "com.activision.callofduty.shooter", "com.dts.freefireth", "com.dts.freefiremax",
            "com.ea.gp.apexlegendsmobilefps", "com.epicgames.fortnite",
            "com.axlebolt.standoff2", "com.criticalforceentertainment.criticalops",
            "com.vng.mlbbvn", "com.garena.game.codm",
            
            // MOBA & Strategy
            "com.mobile.legends", "com.garena.game.kgid",
            "com.riotgames.league.wildrift", "com.riotgames.league.teamfighttactics",
            "com.supercell.clashofclans", "com.supercell.clashroyale", "com.supercell.brawlstars",
            "com.igg.android.lordsmobile", "com.lilithgame.roc.gp",
            "com.im30.ROE.gp", "com.netease.g93na",
            
            // RPG & Adventure  
            "com.miHoYo.GenshinImpact", "com.miHoYo.hkrpgoversea",
            "com.YoStarEN.Arknights", "com.YoStarJP.AzurLane",
            "com.bandainamcoent.dblegends_ww", "com.bandainamcoent.narutox",
            "com.square_enix.android_googleplay.FFBEWW",
            "com.nexon.bluearchive", "com.ea.gp.starwarscapital",
            
            // Casual & Puzzle
            "com.king.candycrushsaga", "com.king.candycrushsodasaga",
            "com.king.candycrush4", "com.supercell.haydaypop",
            "com.kiloo.subwaysurf", "com.halfbrick.fruitninjafree",
            "com.ea.game.pvzfree_row", "com.ea.game.pvz2_row",
            "com.scopely.monopolygo", "com.playgendary.tom",
            "com.innersloth.spacemafia", "com.midasplayer.apps",
            
            // Sports & Racing
            "com.ea.gp.fifamobile", "com.ea.gp.nbamobile", "com.ea.gp.madden20mobile",
            "jp.konami.pesam", "com.naturalmotion.customstreetracer2",
            "com.gameloft.android.ANMP.GloftA8HM", "com.ea.game.nfs14_row",
            "com.fingersoft.hillclimb", "com.dena.a12025452",
            
            // Fighting & Action
            "com.wb.goog.mkx", "com.wb.goog.injustice.bttgames2017",
            "com.nekki.shadowfight", "com.nekki.shadowfight3",
            "com.ChillyRoom.DungeonShooter",
            
            // Sandbox & Simulation
            "com.mojang.minecraftpe", "com.roblox.client",
            "com.ea.game.simcitymobile_row", "com.halfbrick.dantheman",
            "com.nianticlabs.pokemongo", "com.nianticlabs.harrypotterwu",
            
            // Classic Games
            "com.rockstargames.gtasa", "com.rockstargames.gtavc", "com.rockstargames.gtaiii",
            "com.ea.game.simsfreeplay_row",
            
            // Other Popular Games
            "com.playrix.gardenscapes", "com.playrix.homescapes",
            "com.outfit7.mytalkingtomfree", "com.outfit7.mytalkingtom2",
            "com.zhiliaoapp.musically", "sg.bigo.live",
            "com.nintendo.zaga", "com.sega.sonic1px"
        )
        
        // Entertainment & Streaming (Comprehensive)
        val ENTERTAINMENT_APPS = hashSetOf(
            // Video Streaming
            "com.google.android.youtube", "com.android.youtube",
            "app.revanced.android.youtube", "com.google.android.youtube.tv",
            "com.netflix.mediaclient", "com.amazon.avod.thirdpartyclient",
            "com.hulu.plus", "com.disney.disneyplus", "com.hbo.hbonow",
            "com.showtime.standalone", "com.plexapp.android",
            "tv.twitch.android.app", "com.vimeo.android.videoapp",
            "com.crunchyroll.crunchyroid", "com.funimation.funimation",
            "com.paramount.paramountplus", "com.cbs.app",
            "com.nbc.nbcuniversal", "air.tv.douyu.android",
            
            // Music Streaming
            "com.spotify.music", "com.apple.android.music",
            "com.soundcloud.android", "com.audiomack",
            "com.shazam.android", "com.tidal", "com.deezer.android",
            "com.pandora.android", "com.amazon.mp3",
            "com.google.android.apps.youtube.music",
            "com.aspiro.tidal", "fm.last.android",
            
            // Podcasts & Audio
            "com.audible.application", "com.google.android.apps.podcasts",
            "fm.player.radio", "tunein.player",
            "com.bambuna.podcastaddict",
            
            // Live Streaming & Short Videos
            "com.ss.android.ugc.trill", "com.zhiliaoapp.musically",
            "sg.bigo.live", "tv.periscope.android",
            "com.meitu.wheecam", "com.sgiggle.production",
            
            // Regional Streaming
            "com.iheartradio.android", "com.gaana",
            "com.jiosaavn.android", "com.wynk.music",
            "com.hungama.myplay.activity"
        )
        
        // Shopping & E-commerce (Comprehensive)
        val SHOPPING_APPS = hashSetOf(
            // Major E-commerce
            "com.amazon.mShop.android.shopping", "com.ebay.mobile",
            "com.alibaba.aliexpresshd", "com.alibaba.intl.android.apps.poseidon",
            "wish.client.android", "com.contextlogic.wish",
            "com.walmart.android", "com.target.ui",
            "com.etsy.android", "com.shopify.mobile",
            
            // Regional E-commerce (Asia)
            "com.shopee.id", "com.shopee.ph", "com.shopee.sg", "com.shopee.tw",
            "com.lazada.android", "com.lazada.lazada_id",
            "in.amazon.mShop.android.shopping",
            "com.jingdong.app.mall", "com.taobao.taobao",
            "flipkart.android", "com.myntra.android",
            
            // Food Delivery
            "com.ubercab.eats", "com.grubhub.android",
            "com.dd.doordash", "com.postmates.android",
            "com.zomato.android", "com.application.zomato",
            "com.swiggy.android", "com.foodpanda.android",
            "com.app.eatsure", "com.dominos.android",
            
            // Ride Sharing
            "com.ubercab", "com.lyft", "com.gojek.app",
            "com.grab.passenger", "com.olacabs.customer",
            
            // Travel & Booking
            "com.booking", "com.airbnb.android",
            "com.expedia.bookings", "com.agoda.mobile.consumer",
            "com.tripadvisor.tripadvisor",
            
            // Deals & Coupons
            "com.yelp.android", "com.groupon",
            "com.retailmenot.app", "com.honey",
            
            // Fashion & Apparel
            "com.shein.android", "com.nike.plusandroid",
            "com.hm.goe", "com.zara.android",
            
            // Other Shopping
            "com.amazon.dee.app", "com.paypal.android.p2pmobile",
            "com.ikea.app", "com.homedepot.android"
        )
        
        // Dating & Relationships (Comprehensive)
        val DATING_APPS = hashSetOf(
            // Popular Dating Apps
            "com.tinder", "com.bumble.app", "com.hinge.app", "co.hinge.app",
            "com.okcupid", "com.match.android", "com.pof.android",
            "com.badoo.mobile", "com.lovoo", "com.happn.app",
            "com.coffeemeetsbagel", "com.zoosk.zooskmobile",
            "com.eharmony.app", "com.ourtime.android",
            
            // LGBTQ+ Dating
            "com.grindrapp.android", "com.her.app",
            "net.shaadi.android", "com.blued.international",
            
            // Social/Meet People
            "com.meetme", "com.skout.android",
            "com.spark.networks", "com.ft.jsb",
            "com.myyearbook.m", "com.plenty.fish",
            
            // Regional Dating
            "com.muzmatch.muzmatchapp", "com.matrimony.bharat",
            "com.jeevansathi.matrimony", "com.shaadi.android",
            
            // Video Chat Dating
            "com.chatroulette", "com.yubo",
            "com.likeme.android", "com.azarlive.android"
        )
        
        // News & Media (Comprehensive)
        val NEWS_APPS = hashSetOf(
            // News Aggregators
            "com.google.android.apps.magazines", "flipboard.app",
            "com.apple.android.news", "news.androidtv.app",
            "com.smartnews.android", "com.microsoft.amp.apps.bingnews",
            
            // Major News Outlets
            "com.cnn.mobile.android.phone", "com.foxnews.android",
            "com.msnbc.msnbc", "bbc.mobile.news.ww",
            "com.nytimes.android", "com.washingtonpost.android",
            "com.theguardian", "com.huffingtonpost.android",
            "com.usatoday.android.news", "com.ap.news",
            
            // Social News & Discussion
            "com.reddit.frontpage", "com.medium.reader",
            "com.buzzfeed.android", "com.quora.android",
            
            // Business & Finance News
            "com.bloomberg.android", "com.wsj.reader",
            "com.ft.news", "com.economist.lamarr",
            "com.thestreet.streetesmartphone",
            
            // Tech News
            "com.google.android.apps.tachyon",
            "com.aol.mobile.aolapp",
            
            // Regional News
            "com.timesofindia.toi", "com.ndtv.ndtvnews",
            "in.startv.hotstar", "com.aljazeera.mobile"
        )
        
        val SOCIAL_MEDIA_APPS = hashSetOf(
            "com.instagram.android",
            "com.android.youtube",
            "com.facebook.katana",
            "com.facebook.lite",
            "com.instagram.lite",
            "com.instagram.barcelona",
            "com.trassion.infinix.xclub",
            "com.twitter.android",
            "com.xingin.xhs",
            "com.hdvideodownloader.downloaderapp",
            "omegle.tv",
            "in.mohalla.sharechat",
            "sg.bigo.live",
            "com.snapchat.android",
            "com.tiktok.android",
            "com.zhiliaoapp.musically",
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.telegram.messenger.lite",
            "com.pinterest",
            "com.pinterest.lite",
            "com.linkedin.android",
            "com.skype.raider",
            "com.viber.voip",
            "com.discord",
            "com.discord.lite",
            "com.reddit.frontpage",
            "com.kakao.talk",
            "com.kakao.story",
            "jp.naver.line.android",
            "com.naver.band",
            "com.tumblr",
            "com.wechat",
            "com.vk.android",
            "ru.ok.android",
            "com.quora.android",
            "com.byteplus.vivo",
            "com.yalla.chat",
            "com.bereal.ft",
            "com.houseparty",
            "com.micropixels.revolution",
            "com.hellotalk",
            "com.lovoo",
            "com.hinge.app",
            "com.badoo.mobile",
            "com.match.android",
            "com.tinder",
            "com.grindrapp.android",
            "com.bumble.app",
            "co.hinge.app",
            "com.okcupid",
            "com.zoosk.zooskmobile",
            "com.coffeemeetsbagel",
            "com.mewe",
            "com.nextdoor",
            "com.weverse",
            "com.rallyverse.android",
            "com.flickr.android",
            "com.trovo.live",
            "com.kwai.video",
            "com.kwai.video.lite",
            "com.imo.android.imoim",
            "com.starmakerinteractive.starmaker",
            "com.smule.singandroid",
            "com.clubhouse",
            "com.signal.android",
            "com.wire.android",
            "com.threema.app",
            "ch.threema.app",
            "com.vk.videocall",
            "com.tango.me",
            "com.azarlive.android",
            "com.chatous.android",
            "com.kik.android",
            "com.wapa.live",
            "com.boo.snapchat",
            "com.bottledapp",
            "com.wolf.android",
            "com.wattpad",
            "com.live.me",
            "com.chatroulette",
            "com.periscope.android",
            "com.yubo",
            "com.likeme.android",
            "com.imo.android.imolite",
            "com.basketballsocial",
            "com.triller.droid",
            "com.vimeo.android.videoapp",
            "com.streamlabs",
            "com.twitch.android",
            "tv.dlive.app",
            "tv.pangomobile",
            "com.kumu.live",
            "com.afreecatv.activity",
            "com.mixcloud.player",
            "com.poco.android",
            "com.douyin.global",
            "com.like.video",
            "com.dlive.player",
            "com.mobcrush.android",
            "com.huya.kiwi",
            "com.yy.live",
            "com.spoonme",
            "com.younow.android",
            "com.ml.live",
            "com.livekick",
            "com.hulu.plus",
            "com.netflix.mediaclient",
            "com.amazon.avod.thirdpartyclient",
            "com.apple.android.music",
            "com.spotify.music",
            "com.soundcloud.android",
            "com.audiomack",
            "com.shazam.android",
            "com.tidal",
            "com.deezer.android",
            "com.pandora.android",
            "com.iheartradio.android",
            "com.audible.application",
            "com.google.android.apps.podcasts",
            "com.sirius",
            "com.gaana",
            "com.jiosaavn.android",
            "com.wynk.music",
            "com.hungama.myplay.activity",
            "com.apple.android.musiclite",
            "com.meditativeapps.timer",
            "com.truecaller",
            "com.dingtone",
            "com.textmeinc.textme",
            "com.gogii.textplus",
            "com.juphoon.justalk",
            "com.bark",
            "com.talkatone.android",
            "com.tact.chat",
            "com.android.dialer",
            "com.simplemobiletools.contactspro"
        )
        val PRODUCTIVE_APPS = hashSetOf(
            // Note-Taking & Writing
            "com.evernote",
            "com.google.android.keep",
            "com.notion.mobile",
            "com.automattic.simplenote",
            "com.jotterpad.x",
            "com.writeapp.write",

            // Task Management & To-Do
            "com.todoist",
            "com.microsoft.todos",
            "any.do",
            "com.ticktick.task",
            "com.zendesk.asana",
            "com.trello",
            "com.basecamp.bc3",

            // Productivity Suites
            "com.microsoft.office.word",
            "com.microsoft.office.excel",
            "com.microsoft.office.powerpoint",
            "com.google.android.apps.docs.editors.docs",
            "com.google.android.apps.docs.editors.sheets",
            "com.google.android.apps.docs.editors.slides",

            // Calendar & Time Management
            "com.google.android.calendar",
            "com.samsung.android.calendar",
            "com.microsoft.exchange.sync",
            "com.calendly.mobile",
            "com.app.timepage",

            // Communication & Collaboration
            "com.slack",
            "com.microsoft.teams",
            "com.zoom.videomeetings",
            "us.zoom.androidapp",
            "com.cisco.webex.meetings",
            "com.google.android.apps.meet",

            // Cloud Storage & File Management
            "com.google.android.apps.docs",
            "com.dropbox.android",
            "com.box.android",
            "com.microsoft.skydrive",
            "com.synchronoss.dcs.drive",

            // Learning & Skill Development
            "com.duolingo",
            "org.khanacademy.android",
            "com.linkedin.learning",
            "com.skillshare.app",

            // Focus & Meditation
            "com.headspace.android",
            "com.calm.meditation",
            "com.spectratech.insight",

            // Finance & Expense Tracking
            "com.mint",
            "com.quickbooks.android",
            "com.expensify.chat",
            "com.personalcapital.activities",

            // Project Management
            "io.clickup.android",
            "com.asana.app",
            "com.proofhub.mobile",
            "works.shift.app",

            // Writing & Content Creation
            "com.grammarly.android",
            "com.bandlab.bandlab",
            "com.adobe.spark",
            "com.canva.editor",

            // Habit Tracking
            "io.habitica",
            "com.coach.me",
            "com.flourish.habits",

            // Reading & Research
            "com.instapaper.android",
            "com.goodreads",
            "com.pocket.android",

            // Password Management
            "com.lastpass.lpandroid",
            "com.dashlane",
            "com.bitwarden",

            // Other Specialized Productivity Tools
            "com.notion.mobile",
            "com.zhiliaoapp.musically",
            "com.google.android.keep",
            "com.microsoft.office.onenote",
            "com.skype.raider"
        )
        
        // Helper function to get category name for a package
        fun getCategoryForPackage(packageName: String): String? {
            return when {
                GAMING_APPS.contains(packageName) -> "gaming"
                SOCIAL_MEDIA_APPS.contains(packageName) -> "social_media"
                ENTERTAINMENT_APPS.contains(packageName) -> "entertainment"
                DATING_APPS.contains(packageName) -> "dating"
                SHOPPING_APPS.contains(packageName) -> "shopping"
                NEWS_APPS.contains(packageName) -> "news"
                PRODUCTIVE_APPS.contains(packageName) -> "productive"
                else -> null
            }
        }
        
        // Enhanced category detection using ApplicationInfo.category
        fun getCategoryForPackage(packageName: String, appInfo: android.content.pm.ApplicationInfo?): String? {
            // PRIMARY: Use system category (API 26+) - most accurate
            if (appInfo != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val systemCategory = when (appInfo.category) {
                    android.content.pm.ApplicationInfo.CATEGORY_GAME -> "gaming"
                    android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "social_media"
                    android.content.pm.ApplicationInfo.CATEGORY_AUDIO,
                    android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> "entertainment"
                    android.content.pm.ApplicationInfo.CATEGORY_NEWS -> "news"
                    android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "productive"
                    else -> null
                }
                if (systemCategory != null) return systemCategory
            }
            
            // FALLBACK: Check hardcoded lists for known apps
            return getCategoryForPackage(packageName)
        }
        
        fun getAllCategories(): List<Pair<String, String>> {
            return listOf(
                "gaming" to "🎮 Games",
                "social_media" to "📱 Social Media",
                "entertainment" to "🎬 Entertainment",
                "dating" to "❤️ Dating Apps",
                "shopping" to "🛒 Shopping",
                "news" to "📰 News & Media",
                "productive" to "💼 Productive Apps"
            )
        }
    }
}