package kr.co.kokono.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.co.kokono.querydsl.entity.Member;
import kr.co.kokono.querydsl.entity.QMember;
import kr.co.kokono.querydsl.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static kr.co.kokono.querydsl.entity.QMember.member;
import static kr.co.kokono.querydsl.entity.QTeam.team;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {

        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() throws Exception {
        //given
        String qlString = "select m from Member m "
                + "where m.userName = :userName";

        //when
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("userName", "member1")
                .getSingleResult();

        //then
        assertEquals(findMember.getUserName(), "member1");
    }

    @Test
    public void startQueryDsl() throws Exception {
        //given
        QMember m = new QMember("m1");
        //when
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.userName.eq("member1"))
                .fetchOne();
        //then
        assert findMember != null;
        assertEquals(findMember.getUserName(), "member1");
    }

    @Test
    public void search() throws Exception {
        //given

        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.userName.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        //then
        assert findMember != null;
        assertEquals(findMember.getUserName(), "member1");
        /**
         * JPQL이 제공하는 모든 검색 조건 제공
         * member.username.eq("member1") // username = 'member1'
         * member.username.ne("member1") //username != 'member1'
         * member.username.eq("member1").not() // username != 'member1'
         * member.username.isNotNull() //이름이 is not null
         * member.age.in(10, 20) // age in (10,20)
         * member.age.notIn(10, 20) // age not in (10, 20)
         * member.age.between(10,30) //between 10, 30
         * member.age.goe(30) // age >= 30
         * member.age.gt(30) // age > 30
         * member.age.loe(30) // age <= 30
         * member.age.lt(30) // age < 30
         * member.username.like("member%") //like 검색
         * member.username.contains("member") // like ‘%member%’ 검색
         * member.username.startsWith("member") //like ‘member%’ 검색
         */
    }

    @Test
    public void searchAndParam() throws Exception {
        //given

        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.userName.eq("member1"),
                        member.age.eq(10))
                .fetchOne();

        //then
        assert findMember != null;
        assertEquals(findMember.getUserName(), "member1");
    }

    @Test
    public void resultFetch() throws Exception {

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch(); //리스트 조회

        Member fetchOne = queryFactory
                .selectFrom(QMember.member)
                .fetchOne();//단건 조회

        Member fetchFirst = queryFactory
                .selectFrom(QMember.member)
                .fetchFirst(); //처음 한건조회

        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .fetchResults(); //페이징에서 이용할 수 있는 셀렉트쿼리 + 카운트 쿼리

        long count = queryResults.getTotal();
        List<Member> content = queryResults.getResults();

        long fetchCount = queryFactory
                .selectFrom(member)
                .fetchCount(); //카운트 쿼리가 나감
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() throws Exception {
        //given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.userName.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        //then
        assertEquals(member5.getUserName(), "member5");
        assertEquals(member6.getUserName(), "member6");
        assertNull(memberNull.getUserName());
    }

    @Test
    public void paging() throws Exception {
        //given

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.userName.desc())
                .offset(0)
                .limit(2)
                .fetch();
        //then
        assertEquals(result.size(), 2);
        assertEquals(result.get(1).getUserName(), "member3");
    }

    /**
     * JPQL
     * select
     * COUNT(m), //회원수
     * SUM(m.age), //나이 합
     * AVG(m.age), //평균 나이
     * MAX(m.age), //최대 나이
     * MIN(m.age) //최소 나이
     * from Member m
     */
    @Test
    public void aggregation() throws Exception {
        //given

        //when
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                ).from(member)
                .fetch();
        //then
        Tuple tuple = result.get(0);
        assertEquals(tuple.get(member.count()), 4);
        assertEquals(tuple.get(member.age.sum()), 100);
        assertEquals(tuple.get(member.age.avg()), 25);
        assertEquals(tuple.get(member.age.max()), 40);
        assertEquals(tuple.get(member.age.min()), 10);
    }

    @Test
    public void group() throws Exception {
        //given

        //when
        List<Tuple> tuples = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        //then
        Tuple teamA = tuples.get(0);
        Tuple teamB = tuples.get(1);

        assertEquals(teamA.get(team.name), "teamA");
        assertEquals(teamA.get(member.age.avg()), 15);

        assertEquals(teamB.get(team.name), "teamB");
        assertEquals(teamB.get(member.age.avg()), 35);
    }

    @Test
    public void join() throws Exception {
        //given

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        //then
        assertEquals(result.get(0).getUserName(), "member1");
        assertEquals(result.get(1).getUserName(), "member2");
    }

    /**
     * 세타 조인(연관관계가 없는 필드로 조인)
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        //when
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.userName.eq(team.name))
                .fetch();
        //then
        assertEquals(result.get(0).getUserName(), "teamA");
        assertEquals(result.get(1).getUserName(), "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and
     * t.name='teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {
        //given

        //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();
        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 2. 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.userName.eq(team.name))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception {
        //given
        em.flush();
        em.clear();

        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.userName.eq("member1"))
                .fetchOne();

        assert findMember != null;
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        //then
        assertFalse(loaded);
    }

    @Test
    public void fetchJoinUse() throws Exception {
        //given
        em.flush();
        em.clear();

        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team).fetchJoin()
                .where(member.userName.eq("member1"))
                .fetchOne();

        assert findMember != null;
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        //then
        assertTrue(loaded);
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");
        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        //then
        assertEquals(result.get(0).getAge(), 40);
    }

    /**
     * 나이가 평균 나이 이상인 회원
     */
    @Test
    public void subQueryGoe() throws Exception {
        //given
        QMember m2 = new QMember("m2");

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(m2.age.avg())
                                .from(m2)
                ))
                .fetch();

        //then
        assertEquals(result.size(), 2);
    }

    @Test
    public void basicCase() throws Exception {
        //given

        //when
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                ).from(member)
                .fetch();
        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    
    @Test
    public void constant() throws Exception {
        //given
        em.flush(); em.clear();
        
        //when
        Tuple result = queryFactory
                .select(member.userName, Expressions.constant("A"))
                .from(member)
                .fetchFirst();


        String result2 = queryFactory
                .select(member.userName.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.userName.eq("member1"))
                .fetchOne();
        //then
        System.out.println("=====================================");
        System.out.println("result = " + result);
        System.out.println("result2 = " + result2);
        System.out.println("=====================================");
    }
}
