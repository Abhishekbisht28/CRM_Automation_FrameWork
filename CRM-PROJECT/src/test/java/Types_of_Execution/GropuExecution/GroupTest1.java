package Types_of_Execution.GropuExecution;

import org.testng.annotations.Test;

public class GroupTest1 {

    @Test(groups = {"smoke"})
    public void smokeTest1() {
        System.out.println("Hey, I am from smoke group ");
    }

    @Test(groups = {"smoke"})
    public void smokeTest2() {
        System.out.println("Hey, I am from smoke group ");
    }

    @Test(groups = {"san"})
    public void smokeTest3() {
        System.out.println("Hey, I am from smoke group ");
    }

    @Test(groups = {"reg"})
    public void smokeTest4() {
        System.out.println("Hey, I am from smoke group ");
    }
}
