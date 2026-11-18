#include <algorithm>
#include <array>
#include <iostream>
#include <random>
#include <string>
#include <tuple>
#include <vector>

struct Cell { int letter=0, dir=0; };
struct Point { int row, col; };
struct Bounds { int r0, r1, c0, c1; };
struct Placement { int row, col, direction; };
struct Grid {
    static constexpr int size=83;
    std::array<std::array<Cell,size>,size> cells{};
    std::array<std::vector<Point>,26> matching;
    std::array<std::array<std::array<int,2>,size>,size> seen{};
    int iteration=0;
    int hits=0;
    Bounds bounds{size/2,size/2,size/2,size/2};

    void write(const std::string& word, Placement p) {
        const int dr=p.direction==2, dc=1-dr;
        for (int i=0;i<(int)word.size();i++) {
            int r=p.row+i*dr,c=p.col+i*dc,l=word[i]-'A';
            auto& target=cells[r][c];
            if (target.dir) hits++;
            else matching[l].push_back({r,c});
            target.letter=l;
            target.dir|=p.direction;
        }
        bounds={std::min(bounds.r0,p.row),std::max(bounds.r1,p.row+dr*((int)word.size()-1)),
                std::min(bounds.c0,p.col),std::max(bounds.c1,p.col+dc*((int)word.size()-1))};
    }

    int score(Bounds b, int count) const {
        int h=b.r1-b.r0+1,w=b.c1-b.c0+1;
        return h*w + 7*std::max(h,w)+4*std::abs(h-w)-7*count;
    }

    template<class Accept> void options(const std::string& w,int cap,Accept accept) {
        iteration++;
        for (int i=0;i<(int)w.size();i++) {
            for (auto at:matching[w[i]-'A']) {
                int d=3-cells[at.row][at.col].dir;
                if (d==0) continue;
                int dr=d==2,dc=1-dr, r=at.row-i*dr,c=at.col-i*dc;
                if (r<=0 || c<=0 || r+(int)w.size()*dr>=size-1 || c+(int)w.size()*dc>=size-1) continue;
                auto& visited=seen[r][c][d-1];
                if (visited==iteration) continue;
                visited=iteration;
                int length=(int)w.size();
                Bounds after={std::min(bounds.r0,r),std::max(bounds.r1,r+dr*(length-1)),
                              std::min(bounds.c0,c),std::max(bounds.c1,c+dc*(length-1))};
                if (after.r1-after.r0+1>cap || after.c1-after.c0+1>cap) continue;
                if (cells[r-dr][c-dc].dir || cells[r+length*dr][c+length*dc].dir) continue;
                int cross=0;
                bool okay=true;
                for (int j=0;j<length;j++) {
                    int y=r+j*dr,x=c+j*dc;
                    auto old=cells[y][x];
                    if (old.dir) {
                        if ((old.dir&d) || old.letter!=w[j]-'A') {okay=false;break;}
                        cross++;
                    } else if (cells[y+dc][x+dr].dir || cells[y-dc][x-dr].dir) {
                        okay=false;break;
                    }
                }
                if (okay && cross) accept(Placement{r,c,d},score(after,hits+cross));
            }
        }
    }
};

int main(int argc,char** argv) {
    int tries=argc>1?std::stoi(argv[1]):26;
    unsigned seed=argc>2?static_cast<unsigned>(std::stoul(argv[2])):318;
    std::mt19937 random(seed);
    std::uniform_int_distribution<int> variation(-27,27);
    std::vector<std::string> words;
    std::string w;
    while (std::cin>>w) words.push_back(w);
    if (words.empty()) return 1;
    const int n=(int)words.size();
    std::vector<int> order(n);
    for (int i=0;i<n;i++) order[i]=i;
    std::stable_sort(order.begin(),order.end(),[&](int a,int b){return words[a].size()>words[b].size();});
    int best=100000;
    std::vector<Placement> solution;
    for (int attempt=0;attempt<tries;attempt++) {
        Grid grid;
        auto remaining=order;
        std::vector<Placement> placed(n);
        const int first=random()%std::min(5,n);
        int index=remaining[first];
        remaining.erase(remaining.begin()+first);
        placed[index]={Grid::size/2,Grid::size/2,1+(int)(random()%2)};
        grid.write(words[index],placed[index]);
        const int cap=attempt<tries*0.62?23:attempt<tries*0.85?26:30;
        while (!remaining.empty()) {
            int best_choice=100000, best_index=-1;
            Placement position{};
            for (int i=0;i<(int)remaining.size();i++) {
                int id=remaining[i];
                grid.options(words[id],cap,[&](Placement candidate,int score) {
                    score+=variation(random)-2*(int)words[id].size();
                    if (score<best_choice) {
                        best_choice=score;
                        best_index=i;
                        position=candidate;
                    }
                });
            }
            if (best_index<0) break;
            int id=remaining[best_index];
            remaining.erase(remaining.begin()+best_index);
            placed[id]=position;
            grid.write(words[id],position);
        }
        if (!remaining.empty()) continue;
        int value=grid.score(grid.bounds,grid.hits);
        if (value<best) {
            best=value;
            solution=placed;
            for (auto& p:solution) {
                p.row-=grid.bounds.r0;
                p.col-=grid.bounds.c0;
            }
        }
    }
    if (solution.empty()) return 2;
    for (auto p:solution) std::cout<<p.row<<' '<<p.col<<' '<<(p.direction==1?'A':'D')<<'\n';
}
