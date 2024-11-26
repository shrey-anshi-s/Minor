const app = require('./app');
require("dotenv").config();

// Setup database 
const mongoose = require ("mongoose");
mongoose.connect(process.env.DATABASE, {
    useUnifiedTopology: true,
    useNewUrlParser: true,
});

mongoose.connection.on("error", (err) =>{
    console.log("Mongoose Connection Error: " + err.message );
} )

mongoose.connection.once('open', ()=>{
    console.log("MongoDB Connected");
});

//Bring routes 
app.use(require("./routes/user"));

// Bring in models

require('./models/User');
require('./models/User');

app.listen(8000,() => {
    console.log("server listing on port 8000");
})