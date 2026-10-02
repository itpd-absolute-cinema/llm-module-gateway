# Kickoff meeting transcript

**Date:** YYYY-MM-DD
**Participants:** artem, azamat, Customer

Cleaned for readability: filler words, false starts and technical interruptions were removed, and the meaning was kept.
Timestamps are relative to the start of the recording.
Personal and employer names are replaced with `[redacted]`.

[00:00:10] azamat: Will we have a recording of this meeting?
[00:00:17] Customer: Yes, I have started the transcript and the recording.
[00:00:51] Customer: Both are running now, and I can send you the link through Zoom.
[00:01:08] Customer: You are planning to work on the LLM gateway. Did you do any research on that topic?
[00:01:19] artem: Yes, we researched alternatives and developed our vision of the product.
[00:01:32] Customer: I would like to start with your vision. Then I will tell you what I had in mind, and we will synchronize.
[00:01:48] artem: First I want to ask some questions about the project, because we need to understand it.
[00:01:52] Customer: Sure.
[00:02:08] azamat: We are Team 7, and our team name is iTeam7.
[00:02:31] artem: In the first description there was only one use case, filtering data such as personal data.
[00:02:53] artem: Many solutions already exist for that problem.
[00:03:02] Customer: I know, but I included other things too. It is not just filtering data.
[00:03:15] Customer: For example, you may want access control, so that different people have access to different LLMs.
[00:03:30] Customer: Another case is token usage tracking. Many existing products already do this.
[00:03:46] Customer: Another use case is a code base that must never reach any LLM.
[00:03:55] Customer: You fingerprint that code base and detect it as soon as someone tries to send it.
[00:05:29] Customer: The problem is that existing solutions force you into their way of fingerprinting or doing things.
[00:05:47] Customer: The idea of this project is a modular system.
[00:05:52] Customer: For example, you could ask an LLM to build the modules you need on top of it.
[00:06:07] Customer: Many companies have their own logging systems, with their own rules for what is logged and how.
[00:06:19] Customer: Today you take an existing solution and modify it, because you cannot throw away your legacy logging system.
[00:06:30] Customer: So you build adapters between the existing solution and your own requirements.
[00:06:41] Customer: The idea is a project with a plugin system where you build what you need.
[00:06:51] Customer: Does that make sense? Did I answer your question?
[00:06:55] artem: So the point of this project is the simplicity of creating new plugins for validation.
[00:07:09] Customer: Simplicity of creating new plugins for the gateway.
[00:07:13] Customer: I prefer the word "plugins" to "models", because "model" usually means an LLM.
[00:07:30] Customer: We need only the bare minimum: an agent connects to our gateway with authentication, and there is a standard outgoing connection to an LLM such as ChatGPT, Gemini or Claude.
[00:07:55] Customer: We do not care much about the fullness of coverage. We just want to get the idea.
[00:08:08] Customer: Then we build everything else as plugins, and create some ready plugins that people can use, or other plugins based on them.
[00:08:27] artem: Is the gateway supposed to just decline a request if it contains private code or personal data, or what should happen?
[00:08:40] Customer: It depends on the plugin. We want to build a system that enables those plugins.
[00:08:52] Customer: Each company is very different, and existing gateways force you into their way of doing things.
[00:09:03] Customer: Or they are closed source, and you get what you are given and pay for it.
[00:09:19] Customer: The difference I hope for is that a company can look at the system and easily add or replace the plugins it needs.
[00:09:36] Customer: One company will just ban a user who should not use something.
[00:09:44] Customer: Another company will want to log what the user tried to do and then ban them, or allow it and flag it.
[00:09:59] Customer: That should be configurable. Does that make sense?
[00:10:02] artem: Yes.
[00:10:15] artem: There are always limits to what plugins can do. Can a plugin analyze the response from the LLM as well as the request, for example to exclude data from the response?
[00:10:37] Customer: Yes, definitely request and response.
[00:10:43] Customer: This is one of the main things I hear companies ask for.
[00:10:51] Customer: They want to analyze responses, for example to compare models after switching, by how wordy the responses are.
[00:11:14] Customer: Some companies do not want to log responses at all.
[00:11:19] Customer: But we should not block anything that comes in or goes out of the model.
[00:11:31] artem: What about routing? The gateway could also choose a suitable model for a request.
[00:11:45] Customer: That would be good in my mind.
[00:11:51] Customer: One approach is to use a small classifier model that sorts the request by type and decides which model to use.
[00:12:30] Customer: For example, you do not need to spend money on the smartest model if a cheap model can handle the request.
[00:12:45] Customer: In order of priority, first comes the user request. We should intercept it and be able to plug in.
[00:13:00] Customer: Second is what comes out of the LLM.
[00:13:08] Customer: Third is the ability to route, either by configuring routing or by writing plugins that route requests.
[00:13:23] artem: Is it acceptable to restart the gateway after installing a plugin?
[00:13:32] Customer: Yes, I think that is fine.
[00:13:38] Customer: I do not think adding plugins on the fly is a good idea. It is a server-side program, so you should be careful about what gets executed.
[00:14:00] Customer: It is probably better to stop it, create another instance and reload it.
[00:14:10] Customer: Loading on the fly would add unnecessary complexity and security risks.
[00:14:30] Customer: Some local agent tools load plugins on the fly, but they run on your own machine.
[00:14:50] Customer: You know that you added a plugin, and no other user is surprised that something changed.
[00:15:00] Customer: A server has many users connected, so stopping and restarting the system is fine.
[00:15:11] artem: So the plugins will be generated by coding agents, right?
[00:15:22] Customer: I think so. You can write them by hand if you want.
[00:15:31] artem: What about tests for these plugins? Should they also be generated by AI?
[00:15:42] Customer: I think that is how it will be done.
[00:15:48] Customer: The idea is that we provide something that an IT department can take and add plugins to, depending on what they do.
[00:16:00] Customer: If they do not write tests, that is their problem. They wrote a bad plugin and installed it.
[00:16:15] Customer: We cannot force them to write tests.
[00:16:31] artem: That is all for my questions. I have some additional ones, but they are not necessary.
[00:16:39] azamat: I have questions too.
[00:16:51] azamat: Do you expect the server to hold the provider tokens for the LLMs, or to proxy requests with the users' own tokens?
[00:17:28] Customer: That is a very good and important question.
[00:17:34] Customer: I envisioned that the server contains the tokens, because I was thinking of such a system for a company.
[00:17:50] Customer: A company has corporate accounts and tokens for different clouds, and it does not want to give them to everyone.
[00:18:40] Customer: I assumed that IT departments would install and maintain the tokens.
[00:18:50] Customer: Access to the server would also require authentication from the client side.
[00:19:06] Customer: For example, some employees can use only one provider, some can use another provider but only its cheapest model, and one employee can use all models.
[00:19:30] Customer: Please assess what is possible, because the client's vision and what you can actually deliver can be very different.
[00:19:50] Customer: I would be happy if this were configurable, starting from a very simple login that can be extended later.
[00:20:02] Customer: Usually you do not store tokens on the server. You contact a token management system through an API.
[00:20:20] Customer: But we can make it simple and store them on the system, with a security warning in big letters.
[00:20:36] azamat: Should users also be able to use their own tokens?
[00:20:50] azamat: I have experience with an LLM gateway at [redacted].
[00:21:00] azamat: It is used to check whether personal data is sent to LLMs such as ChatGPT or Claude, and to sanitize it.
[00:21:30] azamat: There we use our personal accounts through the corporate gateway.
[00:21:51] Customer: I think it is a valid use case, but it amazes me.
[00:22:00] Customer: Unless you give me a benefit, why would I connect through your servers and not directly to ChatGPT?
[00:22:15] Customer: The benefit to the company is that it can watch what the user does.
[00:22:30] azamat: In our case, otherwise security will come and complain.
[00:22:50] Customer: That is why such a service is needed. People do it because doing it right is hard.
[00:23:05] Customer: The proper way is to have a pool of company keys.
[00:23:15] Customer: For example, you have five keys and ten people, and you route requests through the keys round robin or randomly.
[00:23:35] Customer: People never need to know the keys, and they just access your system.
[00:23:50] Customer: Whether this is legal depends on the provider.
[00:24:01] Customer: What you described is the next best thing, because we do not have anything better.
[00:24:09] Customer: We can add it as a feature, but I feel we would be building a bad use case into the system.
[00:24:20] Customer: If we build it in, it will be used, so why encourage it?
[00:24:37] azamat: In my company a platform agent uses the gateway automatically, and most people do not install other tools themselves.
[00:25:00] azamat: For most people it is not about security but about comfort.
[00:25:40] Customer: You are talking about agents, which is a different thing.
[00:25:50] Customer: With an agent, you download it and assign the right key at install time on the client side.
[00:26:10] Customer: The other option is to decide at the gateway level.
[00:26:32] Customer: If you feel strongly about it, we can add it. The system is supposed to be extendable, so if it exists, why not.
[00:26:45] azamat: At least security can always turn it off.
[00:26:50] Customer: Yes.
[00:27:17] artem: What types of LLM API should we support?
[00:27:23] Customer: Let us go with the most standard ones.
[00:27:30] Customer: It would be nice if supporting another API type were a plugin that converts formats, but that is the tricky part.
[00:28:06] Customer: I am not aiming for breadth. One or two providers are enough for me.
[00:28:20] Customer: I personally use Gemini. The OpenAI standard and Claude would also be fine.
[00:28:48] azamat: Do you have a preference for the implementation language?
[00:28:56] azamat: It would be strange to use something like Haskell, because plugins would be harder to create.
[00:29:18] Customer: Any language is fine.
[00:29:25] Customer: I do have a preference, Python if possible. JavaScript is fine too, now that agents write the code.
[00:29:40] Customer: I would strongly discourage Haskell for this.
[00:29:44] Customer: Python is good on the server, especially with a plugin system, but it is slow.
[00:30:05] Customer: For a big company performance may be a problem, although you can write a good system in Python with asynchronous code.
[00:30:30] Customer: Rust is fine. Go is fine. I would say no to C++ if the only reason is religious.
[00:30:57] artem: What about Java?
[00:30:58] Customer: I would prefer to avoid Java and JavaScript, but if that is what you know, it is fine.
[00:31:20] artem: Many backend services use it.
[00:31:25] Customer: That alone is not a strong argument.
[00:31:35] Customer: I have nothing against Java, but I would survive if you write it yourselves.
[00:32:18] Customer: What languages are you planning to use?
[00:32:25] azamat: All of our teammates know Python.
[00:32:35] azamat: My own preference would be Go, but it may be hard for the other teammates, so I think we will prefer Python.
[00:33:04] Customer: Go or Python are both fine and approved.
[00:33:12] Customer: I understand why you would use Go, for performance, and why you would use Python, for extendability and the availability of developers.
[00:33:28] artem: For the gateway we will use Java anyway.
[00:33:35] artem: For the plugin system we could use another language.
[00:33:40] artem: I am also thinking about our own domain-specific language for plugins, although it may be too hard.
[00:33:53] Customer: The problem with a new domain-specific language is that an LLM will have a hard time writing it.
[00:34:03] Customer: You would need extensive documentation loaded into its context.
[00:34:10] Customer: It would be better to write in something that LLMs already know.
[00:34:15] Customer: Can you at least use Kotlin? It compiles to Java bytecode.
[00:34:30] artem: That could be complicated.
[00:34:40] Customer: Can everyone on your team write Java projects?
[00:34:45] artem: Yes.
[00:34:48] Customer: In that case, fine.
[00:34:55] azamat: Would it be good to give coding agents instructions for creating plugins, such as a skill, an example and an instruction?
[00:35:10] azamat: Then an agent would not have to learn the whole code base to build a plugin.
[00:35:36] Customer: I think that would be good.
[00:35:42] Customer: You have to write for LLMs too, so they know where to look things up.
[00:36:20] Customer: I do not want to send the entire project to an LLM to figure out how it works and how to write plugins for it.
[00:36:35] Customer: I would say it is a nice-to-have feature, and it would be good to practice.
[00:36:58] Customer: What is your first stage, something that works? When do you think you can deliver it, and how will it look?
[00:37:20] Customer: It can be very simple, just the simplest runnable thing.
[00:37:43] azamat: I think it is a proxy that gets the request from the user and adds a token stored in a `.env` file.
[00:37:55] azamat: It then sends the request to one supported provider and returns the result.
[00:38:05] azamat: Before sending, there could be a very simple plugin that masks eight digits in a row as a phone number.
[00:38:26] Customer: When can you deliver that? One week, two, three, ten?
[00:38:29] azamat: I think two weeks, maybe three.
[00:38:44] azamat: At the start I want to create basic rules for the code, such as CI/CD and linters.
[00:38:55] azamat: Teammates have different experience with Python, and I want infrastructure that supports good development later.
[00:39:20] azamat: So the approach is not to deliver as fast as possible, but to spend some time on preparation first.
[00:39:41] Customer: I understand. Sharpen your saw.
[00:40:05] azamat: The course says we can use any AI tool, for research, writing and coding.
[00:40:20] azamat: For us it will be less about coding and more about validating what agents did and managing them. Is that okay?
[00:40:48] Customer: Yes, that is how work is done now, and with this course we try to do it the way it is done at a workplace.
[00:41:05] Customer: It is more important that you can build a product from the idea stage to a usable stage, and then improve it in stages.
[00:41:25] Customer: That includes when you add analytics and how you interact with the client.
[00:41:40] Customer: For the rest there are agents, and we have to learn to be effective with them.
[00:41:50] Customer: Our role is higher now. It is about product skills more than programming skills.
[00:42:24] Customer: Use AI the way you would use it at work.
[00:42:57] Customer: This is one reason your code has to be open-source-like.
[00:43:10] Customer: With closed code you would have to ask whether you can use an LLM, what the licenses are, and whether you broke a non-disclosure agreement by sending the material to a third party.
[00:43:40] Customer: One group that cannot use an LLM online would be much slower than another group that can, and the results would be very different.
[00:44:17] artem: How many plugins are expected to be installed in the gateway?
[00:44:27] Customer: That is a very good question.
[00:44:35] Customer: Depending on the answer, the plugin system has to be very different.
[00:44:45] Customer: I did not have a number. I expected maybe a couple of hundred, but why not a million.
[00:44:57] Customer: It would be good if you looked at your architecture and stated the upper limit that follows from the way you build it.
[00:45:20] Customer: It could be a hundred plugins, and more would still work, but things would start slowing down because of some decision.
[00:45:35] Customer: That analysis should be part of the project.
[00:45:45] azamat: It is not about having 100 plugins, it is about being able to create 100 plugins in one hour.
[00:45:55] Customer: You can create them, but how will they run in the same system?
[00:46:05] Customer: Your system has to handle messaging between plugins, which can become complicated.
[00:46:20] Customer: For a 10-week course you might say that you picked this architecture, so the limit is 100 plugins running at the same time.
[00:46:41] Customer: If you specify the limitation, that would be good, and it can be anything.
[00:46:50] Customer: I am not looking for industrial-grade software at this point.
[00:46:58] Customer: This course prepares you for the industrial project next semester, in January.
[00:47:15] Customer: It is good that you are thinking about it, because that is exactly what you should think about for an industrial setting.
[00:47:38] Customer: We meet next time on Friday, the same way and at the same time.
[00:47:50] artem: Yes, I believe so.
[00:48:00] Customer: For everything else, write in the channel that I will create.
[00:48:10] Customer: There will be a general channel for everyone, and you can put specific questions about your projects there.
[00:48:31] Customer: Thank you very much. I am looking forward to seeing your products. It is going to be cool.
