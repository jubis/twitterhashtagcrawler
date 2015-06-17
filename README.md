Twitter hashtag crawler
=======================

[ ![Codeship Status for jubis/twitterhashtagcrawler](https://codeship.com/projects/4e521890-4749-0132-51c5-7a1106078ecc/status?branch=master)](https://codeship.com/projects/45563)

Visit running application: http://hashtag.matiass.me/api/status

Run
---
The application cannot be started without proper Twitter credentials. You have to first create a Twitter application and then generate an access token for it.

```
sbt run  
    -Dtwitter4j.oauth.consumerKey=<twitter consumer key> 
    -Dtwitter4j.oauth.consumerSecret=<twitter consumer secret>      
    -Dtwitter4j.oauth.accessToken=<twitter access token> 
    -Dtwitter4j.oauth.accessTokenSecret=<twitter access token secret>
```
