# project

This is the project, a RESTful web service with the basic features for one or more checking accounts. The solution uses the basic architecture of a REST Web application, using the commons HTTP request verbs to:

- POST an operation on a checking account
```
POST "/operation" [id description amount date]
```
- GET a specific account balance
```
GET "/account-balance" [id]
```
- GET the bank statement of a specific account
```
GET "/account-bank-statement" [id]
```
- GET periods of negative balances of a specific account
```
GET "/account-negative-periods" [id]
```
The project-nu solution is also able to support the insertion of different account operations and organize these operations into their respective accounts references. To verify accounts that have operations listed, you can use:

```
GET "/accounts-id" []
```
The application is also capable of getting the daily bank balance with the information of all operations. With that, the bank can have access to all the transactions of one day and the money available in the institution.

```
GET "/bank-daily-balance" []
```

All of these solutions were created using the ref variables of the Clojure programming language. The main structure used is a ref (all accounts), which contains a list of other refs, one for each account. Each account has the follow structure:
```
{:acc x :ops (operation1 operation2 ... operationN)}
```
When the user add a new operation, the software checks if the new operation has a right structure. After the check process, the solution creates your own constructor for an operation:
```
{:id-acc ID, :op-id ID,  :type-op ('debit' or 'credit'),  :desc (description),
 :amount (value), :date (date)}
```
Each operation is added to your respective account ref. Then, the project can work directly with the account. All the other commands to an account, such as GET "/account-balance", just need the account ID to be run. 


## Prerequisites
To follow the **running tutorial** you will need to:
1. download the source code of the repository.
2. Clojure 1.8.0 or above installed. [Clojure] (https://clojure.org/index)
3. Leiningen 2.0.0 or above installed. [leiningen] (https://leiningen.org/)
4. HTTPie a command line HTTP client installed. [HTTPie] (https://httpie.org/doc#usage)

## Running tutorial

After the download of the source code, in the project-nu directory, you can start the application using:

- The **lein run** command
```
lein run
```
- OR building the .jar archive
```
lein uberjar
```
and run in the *project/target* directory

```
java -jar project-nu-0.1.0-SNAPSHOT-standalone.jar
```
With the start of the application, on another terminal, all requests can be sent using the HTTPie commands.

To insert operations:

```
http POST :3000/operation id=1 description="Deposit" amount=1000.00 date=15/10
http POST :3000/operation id=1 description="Purchase on Amazon" amount=3.34 date=16/10
http POST :3000/operation id=1 description="Purchase on Uber" amount=45.23 date=16/10
http POST :3000/operation id=1 description="Withdrawal" amount=180.00 date=17/10
```
To get the account balance:

```
http GET :3000/account-balance id=1
```
To get the account bank-statement

```
http GET :3000/account-bank-statement id=1
```
To get the negative periods of a specific account

```
http POST :3000/operation id=1 description="Purchase on fly ticket" amount=800.00 date=18/10
http POST :3000/operation id=1 description="Deposit" amount=100.00 date=25/10
http POST :3000/operation id=1 description="Withdrawal" amount=200.00 date=27/10

http GET :3000/account-negative-periods id=1
```
In order to test the other features of the project-nu application you can add operations to another account:

```
http POST :3000/operation id=2 description="Purchase on Amazon" amount=500.00 date=16/10
http POST :3000/operation id=2 description="Deposit" amount=6.34 date=15/10
http POST :3000/operation id=2 description="Purchase on Uber" amount=45.23 date=19/10
```
Now, you will be capable of listing all accounts with operations:

```
http GET :3000/accounts-id
```

And to get the bank balance for each day, with all accounts operations, you can use:

```
http GET :3000/bank-daily-balance
```
